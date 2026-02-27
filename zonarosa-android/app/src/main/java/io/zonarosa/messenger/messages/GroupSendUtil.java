package io.zonarosa.messenger.messages;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import io.zonarosa.core.util.logging.Log;
import io.zonarosa.libzonarosa.metadata.certificate.SenderCertificate;
import io.zonarosa.libzonarosa.protocol.InvalidKeyException;
import io.zonarosa.libzonarosa.protocol.InvalidRegistrationIdException;
import io.zonarosa.libzonarosa.protocol.NoSessionException;
import io.zonarosa.libzonarosa.zkgroup.groups.GroupSecretParams;
import io.zonarosa.libzonarosa.zkgroup.groupsend.GroupSendEndorsement;
import io.zonarosa.libzonarosa.zkgroup.groupsend.GroupSendFullToken;
import io.zonarosa.messenger.crypto.SealedSenderAccessUtil;
import io.zonarosa.messenger.crypto.SenderKeyUtil;
import io.zonarosa.messenger.database.MessageSendLogTables;
import io.zonarosa.messenger.database.ZonaRosaDatabase;
import io.zonarosa.messenger.database.model.DistributionListId;
import io.zonarosa.messenger.database.model.GroupRecord;
import io.zonarosa.messenger.database.model.GroupSendEndorsementRecords;
import io.zonarosa.messenger.database.model.MessageId;
import io.zonarosa.messenger.dependencies.AppDependencies;
import io.zonarosa.messenger.groups.GroupChangeException;
import io.zonarosa.messenger.groups.GroupId;
import io.zonarosa.messenger.groups.GroupManager;
import io.zonarosa.messenger.jobs.RequestGroupV2InfoJob;
import io.zonarosa.messenger.keyvalue.ZonaRosaStore;
import io.zonarosa.messenger.recipients.Recipient;
import io.zonarosa.messenger.recipients.RecipientId;
import io.zonarosa.messenger.recipients.RecipientUtil;
import io.zonarosa.messenger.util.RecipientAccessList;
import io.zonarosa.messenger.util.RemoteConfig;
import io.zonarosa.messenger.util.ZonaRosaLocalMetrics;
import io.zonarosa.core.util.Util;
import io.zonarosa.service.api.ZonaRosaServiceMessageSender;
import io.zonarosa.service.api.ZonaRosaServiceMessageSender.LegacyGroupEvents;
import io.zonarosa.service.api.ZonaRosaServiceMessageSender.SenderKeyGroupEvents;
import io.zonarosa.service.api.crypto.ContentHint;
import io.zonarosa.service.api.crypto.SealedSenderAccess;
import io.zonarosa.service.api.crypto.UnidentifiedAccess;
import io.zonarosa.service.api.crypto.UntrustedIdentityException;
import io.zonarosa.service.api.groupsv2.GroupSendEndorsements;
import io.zonarosa.service.api.messages.SendMessageResult;
import io.zonarosa.service.api.messages.ZonaRosaServiceDataMessage;
import io.zonarosa.service.api.messages.ZonaRosaServiceEditMessage;
import io.zonarosa.service.api.messages.ZonaRosaServiceStoryMessage;
import io.zonarosa.service.api.messages.ZonaRosaServiceStoryMessageRecipient;
import io.zonarosa.service.api.messages.ZonaRosaServiceTypingMessage;
import io.zonarosa.service.api.messages.calls.ZonaRosaServiceCallMessage;
import io.zonarosa.service.api.push.DistributionId;
import io.zonarosa.core.models.ServiceId;
import io.zonarosa.service.api.push.ZonaRosaServiceAddress;
import io.zonarosa.service.api.push.exceptions.NotFoundException;
import io.zonarosa.service.api.util.Preconditions;
import io.zonarosa.service.internal.push.exceptions.InvalidUnidentifiedAccessHeaderException;
import io.zonarosa.service.internal.push.http.CancelationZonaRosa;
import io.zonarosa.service.internal.push.http.PartialSendBatchCompleteListener;
import io.zonarosa.service.internal.push.http.PartialSendCompleteListener;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public final class GroupSendUtil {

  private static final String TAG = Log.tag(GroupSendUtil.class);

  private GroupSendUtil() {}


  /**
   * Handles all of the logic of sending to a group. Will do sender key sends and legacy 1:1 sends as-needed, and give you back a list of
   * {@link SendMessageResult}s just like we're used to.
   *
   * Messages sent this way, if failed to be decrypted by the receiving party, can be requested to be resent.
   * Note that the ContentHint <em>may not</em> be {@link ContentHint#RESENDABLE} -- it just means that we have an actual record of the message
   * and we <em>could</em> resend it if asked.
   *
   * @param groupId The groupId of the group you're sending to, or null if you're sending to a collection of recipients not joined by a group.
   * @param isRecipientUpdate True if you've already sent this message to some recipients in the past, otherwise false.
   * @param isForStory True if the message is related to a story, and should be sent with the story flag on the envelope
   */
  @WorkerThread
  public static List<SendMessageResult> sendResendableDataMessage(@NonNull Context context,
                                                                  @Nullable GroupId.V2 groupId,
                                                                  @Nullable DistributionListId distributionListId,
                                                                  @NonNull List<Recipient> allTargets,
                                                                  boolean isRecipientUpdate,
                                                                  ContentHint contentHint,
                                                                  @NonNull MessageId messageId,
                                                                  @NonNull ZonaRosaServiceDataMessage message,
                                                                  boolean urgent,
                                                                  boolean isForStory,
                                                                  @Nullable ZonaRosaServiceEditMessage editMessage,
                                                                  @Nullable CancelationZonaRosa cancelationZonaRosa)
      throws IOException, UntrustedIdentityException
  {
    Preconditions.checkArgument(groupId == null || distributionListId == null, "Cannot supply both a groupId and a distributionListId!");

    DistributionId distributionId = groupId != null ? getDistributionId(groupId) : getDistributionId(distributionListId);

    return sendMessage(context, groupId, distributionId, messageId, allTargets, isRecipientUpdate, isForStory, DataSendOperation.resendable(message, contentHint, messageId, urgent, isForStory, editMessage), cancelationZonaRosa);
  }

  /**
   * Handles all of the logic of sending to a group. Will do sender key sends and legacy 1:1 sends as-needed, and give you back a list of
   * {@link SendMessageResult}s just like we're used to.
   *
   * Messages sent this way, if failed to be decrypted by the receiving party, can *not* be requested to be resent.
   *
   * @param groupId The groupId of the group you're sending to, or null if you're sending to a collection of recipients not joined by a group.
   * @param isRecipientUpdate True if you've already sent this message to some recipients in the past, otherwise false.
   */
  @WorkerThread
  public static List<SendMessageResult> sendUnresendableDataMessage(@NonNull Context context,
                                                                    @Nullable GroupId.V2 groupId,
                                                                    @NonNull List<Recipient> allTargets,
                                                                    boolean isRecipientUpdate,
                                                                    ContentHint contentHint,
                                                                    @NonNull ZonaRosaServiceDataMessage message,
                                                                    boolean urgent,
                                                                    CancelationZonaRosa cancelationZonaRosa)
      throws IOException, UntrustedIdentityException
  {
    return sendMessage(context, groupId, getDistributionId(groupId), null, allTargets, isRecipientUpdate, false, DataSendOperation.unresendable(message, contentHint, urgent), cancelationZonaRosa);
  }

  /**
   * Handles all of the logic of sending to a group. Will do sender key sends and legacy 1:1 sends as-needed, and give you back a list of
   * {@link SendMessageResult}s just like we're used to.
   *
   * @param groupId The groupId of the group you're sending to, or null if you're sending to a collection of recipients not joined by a group.
   */
  @WorkerThread
  public static List<SendMessageResult> sendTypingMessage(@NonNull Context context,
                                                          @Nullable GroupId.V2 groupId,
                                                          @NonNull List<Recipient> allTargets,
                                                          @NonNull ZonaRosaServiceTypingMessage message,
                                                          @Nullable CancelationZonaRosa cancelationZonaRosa)
      throws IOException, UntrustedIdentityException
  {
    return sendMessage(context, groupId, getDistributionId(groupId), null, allTargets, false, false, new TypingSendOperation(message), cancelationZonaRosa);
  }

  /**
   * Handles all of the logic of sending to a group. Will do sender key sends and legacy 1:1 sends as-needed, and give you back a list of
   * {@link SendMessageResult}s just like we're used to.
   *
   * @param groupId The groupId of the group you're sending to
   */
  @WorkerThread
  public static List<SendMessageResult> sendCallMessage(@NonNull Context context,
                                                        @NonNull GroupId.V2 groupId,
                                                        @NonNull List<Recipient> allTargets,
                                                        @NonNull ZonaRosaServiceCallMessage message)
      throws IOException, UntrustedIdentityException
  {
    return sendMessage(context, groupId, getDistributionId(groupId), null, allTargets, false, false, new CallSendOperation(message), null);
  }

  /**
   * Handles all of the logic of sending a story to a distribution list. Will do sender key sends and legacy 1:1 sends as-needed, and give you back a list of
   * {@link SendMessageResult}s just like we're used to.
   *
   * @param isRecipientUpdate True if you've already sent this message to some recipients in the past, otherwise false.
   */
  public static List<SendMessageResult> sendStoryMessage(@NonNull Context context,
                                                         @NonNull DistributionListId distributionListId,
                                                         @NonNull List<Recipient> allTargets,
                                                         boolean isRecipientUpdate,
                                                         @NonNull MessageId messageId,
                                                         long sentTimestamp,
                                                         @NonNull ZonaRosaServiceStoryMessage message,
                                                         @NonNull Set<ZonaRosaServiceStoryMessageRecipient> manifest)
      throws IOException, UntrustedIdentityException
  {
    return sendMessage(
        context,
        null,
        getDistributionId(distributionListId),
        messageId,
        allTargets,
        isRecipientUpdate,
        true,
        new StorySendOperation(messageId, null, sentTimestamp, message, manifest),
        null);
  }

  /**
   * Handles all of the logic of sending a story to a group. Will do sender key sends and legacy 1:1 sends as-needed, and give you back a list of
   * {@link SendMessageResult}s just like we're used to.
   *
   * @param isRecipientUpdate True if you've already sent this message to some recipients in the past, otherwise false.
   */
  public static List<SendMessageResult> sendGroupStoryMessage(@NonNull Context context,
                                                              @NonNull GroupId.V2 groupId,
                                                              @NonNull List<Recipient> allTargets,
                                                              boolean isRecipientUpdate,
                                                              @NonNull MessageId messageId,
                                                              long sentTimestamp,
                                                              @NonNull ZonaRosaServiceStoryMessage message)
      throws IOException, UntrustedIdentityException
  {
    return sendMessage(
        context,
        groupId,
        getDistributionId(groupId),
        messageId,
        allTargets,
        isRecipientUpdate,
        true,
        new StorySendOperation(messageId,
                               groupId,
                               sentTimestamp,
                               message,
                               allTargets.stream()
                                         .map(target -> new ZonaRosaServiceStoryMessageRecipient(new ZonaRosaServiceAddress(target.requireServiceId()),
                                                                                               Collections.emptyList(),
                                                                                               true))
                                         .collect(Collectors.toSet())),
        null);
  }

  /**
   * Handles all of the logic of sending to a group. Will do sender key sends and legacy 1:1 sends as-needed, and give you back a list of
   * {@link SendMessageResult}s just like we're used to.
   *
   * @param groupId The groupId of the group you're sending to, or null if you're sending to a collection of recipients not joined by a group.
   * @param isRecipientUpdate True if you've already sent this message to some recipients in the past, otherwise false.
   */
  @WorkerThread
  private static List<SendMessageResult> sendMessage(@NonNull Context context,
                                                     @Nullable GroupId.V2 groupId,
                                                     @Nullable DistributionId distributionId,
                                                     @Nullable MessageId relatedMessageId,
                                                     @NonNull List<Recipient> allTargets,
                                                     boolean isRecipientUpdate,
                                                     boolean isStorySend,
                                                     @NonNull SendOperation sendOperation,
                                                     @Nullable CancelationZonaRosa cancelationZonaRosa)
      throws IOException, UntrustedIdentityException
  {
    Log.i(TAG, "Starting group send. GroupId: " + (groupId != null ? groupId.toString() : "none") + ", DistributionId: " + (distributionId != null ? distributionId.toString() : "none") + " RelatedMessageId: " + (relatedMessageId != null ? relatedMessageId.toString() : "none") + ", Targets: " + allTargets.size() + ", RecipientUpdate: " + isRecipientUpdate + ", Operation: " + sendOperation.getClass().getSimpleName());

    Set<Recipient>  unregisteredTargets = allTargets.stream().filter(it -> it.isUnregistered() || it.isUnknown()).collect(Collectors.toSet());
    List<Recipient> registeredTargets   = allTargets.stream().filter(r -> !unregisteredTargets.contains(r)).collect(Collectors.toList());

    RecipientData               recipients                     = new RecipientData(context, registeredTargets, isStorySend);
    Optional<GroupRecord>       groupRecord                    = groupId != null ? ZonaRosaDatabase.groups().getGroup(groupId) : Optional.empty();
    GroupSendEndorsementRecords groupSendEndorsementRecords    = groupRecord.filter(GroupRecord::isV2Group).map(g -> ZonaRosaDatabase.groups().getGroupSendEndorsements(g.getId())).orElse(null);
    long                        groupSendEndorsementExpiration = groupRecord.map(GroupRecord::getGroupSendEndorsementExpiration).orElse(0L);
    SenderCertificate           senderCertificate              = SealedSenderAccessUtil.getSealedSenderCertificate();
    boolean                     useGroupSendEndorsements       = groupSendEndorsementRecords != null;

    if (useGroupSendEndorsements && senderCertificate == null) {
      Log.w(TAG, "Can't use group send endorsements without a sealed sender certificate, falling back to access key");
      useGroupSendEndorsements = false;
    } else if (useGroupSendEndorsements) {
      boolean refreshGroupSendEndorsements = false;

      if (groupSendEndorsementExpiration == 0) {
        Log.i(TAG, "No group send endorsements expiration set, need to refresh");
        refreshGroupSendEndorsements = true;
      } else if (groupSendEndorsementExpiration - TimeUnit.HOURS.toMillis(2) < System.currentTimeMillis()) {
        Log.i(TAG, "Group send endorsements are expired or expire imminently, refresh. Expires in " + (groupSendEndorsementExpiration - System.currentTimeMillis()) + "ms");
        refreshGroupSendEndorsements = true;
      } else if (groupSendEndorsementRecords.isMissingAnyEndorsements()) {
        Log.i(TAG, "Missing group send endorsements for some members, refresh.");
        refreshGroupSendEndorsements = true;
      }

      if (refreshGroupSendEndorsements) {
        try {
          GroupManager.updateGroupSendEndorsements(context, groupRecord.get().requireV2GroupProperties().getGroupMasterKey());

          groupSendEndorsementExpiration = ZonaRosaDatabase.groups().getGroupSendEndorsementsExpiration(groupId);
          groupSendEndorsementRecords    = ZonaRosaDatabase.groups().getGroupSendEndorsements(groupId);
        } catch (GroupChangeException | IOException e) {
          if (groupSendEndorsementExpiration == 0) {
            Log.w(TAG, "Unable to update group send endorsements, falling back to legacy", e);
            useGroupSendEndorsements = false;
            groupSendEndorsementRecords = new GroupSendEndorsementRecords(Collections.emptyMap());

            GroupSendEndorsementInternalNotifier.maybePostGroupSendFallbackError(context);
          } else {
            Log.w(TAG, "Unable to update group send endorsements, using what we have", e);
          }
        }

        Log.d(TAG, "Refresh all group state because we needed to refresh gse");
        AppDependencies.getJobManager().add(new RequestGroupV2InfoJob(groupId));
      }
    }

    List<Recipient> senderKeyTargets = new LinkedList<>();
    List<Recipient> legacyTargets    = new LinkedList<>();

    // Determine recipients that can be sent to via sender key vs must use legacy fan-out
    if (distributionId == null) {
      Log.i(TAG, "No DistributionId. Using legacy.");
      legacyTargets.addAll(registeredTargets);
    } else if (isStorySend) {
      Log.i(TAG, "Sending a story. Using sender key for all " + allTargets.size() + " recipients.");
      senderKeyTargets.addAll(registeredTargets);
    } else if (!useGroupSendEndorsements) {
      Log.i(TAG, "No group send endorsements, using legacy for all " + allTargets.size() + " recipients.");
      legacyTargets.addAll(registeredTargets);
    } else {
      for (Recipient recipient : registeredTargets) {
        boolean              validMembership      = groupRecord.get().getMembers().contains(recipient.getId());
        GroupSendEndorsement groupSendEndorsement = groupSendEndorsementRecords.getEndorsement(recipient.getId());

        if (groupSendEndorsement != null && recipient.getHasAci() && validMembership) {
          senderKeyTargets.add(recipient);
        } else {
          legacyTargets.add(recipient);
          if (validMembership) {
            Log.w(TAG, "Should be using group send endorsement but not found for " + recipient.getId());
            GroupSendEndorsementInternalNotifier.maybePostMissingGroupSendEndorsement(context);
          }
        }
      }
    }

    // Enforce minimum number of sender key destinations
    if (ZonaRosaStore.internal().getRemoveSenderKeyMinimum()) {
      Log.i(TAG, "Sender key minimum removed. Using for " + senderKeyTargets.size() + " recipients.");
    } else if (senderKeyTargets.size() < 2 && !isStorySend) {
      Log.i(TAG, "Too few sender-key-capable users (" + senderKeyTargets.size() + ") for non-story send. Doing all legacy sends.");
      legacyTargets.addAll(senderKeyTargets);
      senderKeyTargets.clear();
    } else {
      Log.i(TAG, "Can use sender key for " + senderKeyTargets.size() + "/" + allTargets.size() + " recipients.");
    }

    if (relatedMessageId != null && groupId != null) {
      ZonaRosaLocalMetrics.GroupMessageSend.onSenderKeyStarted(relatedMessageId.getId());
    }

    List<SendMessageResult>    allResults    = new ArrayList<>(allTargets.size());
    ZonaRosaServiceMessageSender messageSender = AppDependencies.getZonaRosaServiceMessageSender();

    if (Util.hasItems(senderKeyTargets) && distributionId != null) {
      long           keyCreateTime  = SenderKeyUtil.getCreateTimeForOurKey(distributionId);
      long           keyAge         = System.currentTimeMillis() - keyCreateTime;

      if (keyCreateTime != -1 && keyAge > RemoteConfig.senderKeyMaxAge()) {
        Log.w(TAG, "DistributionId " + distributionId + " was created at " + keyCreateTime + " and is " + (keyAge) + " ms old (~" + TimeUnit.MILLISECONDS.toDays(keyAge) + " days). Rotating.");
        SenderKeyUtil.rotateOurKey(distributionId);
      }

      try {
        List<ZonaRosaServiceAddress>               targets               = new ArrayList<>(senderKeyTargets.size());
        List<UnidentifiedAccess>                 access                = new ArrayList<>(senderKeyTargets.size());
        Map<ServiceId.ACI, GroupSendEndorsement> senderKeyEndorsements = new HashMap<>(senderKeyTargets.size());
        GroupSendEndorsements                    groupSendEndorsements = null;

        for (Recipient recipient : senderKeyTargets) {
          targets.add(recipients.getAddress(recipient.getId()));

          if (useGroupSendEndorsements) {
            senderKeyEndorsements.put(recipient.requireAci(), groupSendEndorsementRecords.getEndorsement(recipient.getId()));
            access.add(recipients.getAccess(recipient.getId()));
          } else {
            access.add(recipients.requireAccess(recipient.getId()));
          }
        }

        if (useGroupSendEndorsements) {
          groupSendEndorsements = new GroupSendEndorsements(
              groupSendEndorsementExpiration,
              senderKeyEndorsements,
              senderCertificate,
              GroupSecretParams.deriveFromMasterKey(groupRecord.get().requireV2GroupProperties().getGroupMasterKey())
          );
        }

        final MessageSendLogTables messageLogDatabase  = ZonaRosaDatabase.messageLog();
        final AtomicLong           entryId             = new AtomicLong(-1);
        final boolean              includeInMessageLog = sendOperation.shouldIncludeInMessageLog();

        if (cancelationZonaRosa != null && cancelationZonaRosa.isCanceled()) {
          Log.i(TAG, "Send canceled before any sends took place. Returning an empty list.");
          return Collections.emptyList();
        }

        List<SendMessageResult> results = sendOperation.sendWithSenderKey(messageSender, distributionId, targets, access, groupSendEndorsements, isRecipientUpdate, partialResults -> {
          if (!includeInMessageLog) {
            return;
          }

          synchronized (entryId) {
            if (entryId.get() == -1) {
              entryId.set(messageLogDatabase.insertIfPossible(sendOperation.getSentTimestamp(), senderKeyTargets, partialResults, sendOperation.getContentHint(), sendOperation.getRelatedMessageId(), sendOperation.isUrgent()));
            } else {
              for (SendMessageResult result : partialResults) {
                entryId.set(messageLogDatabase.addRecipientToExistingEntryIfPossible(entryId.get(), recipients.requireRecipientId(result.getAddress()), sendOperation.getSentTimestamp(), result, sendOperation.getContentHint(), sendOperation.getRelatedMessageId(), sendOperation.isUrgent()));
              }
            }
          }
        });

        allResults.addAll(results);

        int successCount = (int) results.stream().filter(SendMessageResult::isSuccess).count();
        Log.d(TAG, "Successfully sent using sender key to " + successCount + "/" + targets.size() + " sender key targets.");

        if (relatedMessageId != null) {
          ZonaRosaLocalMetrics.GroupMessageSend.onSenderKeyMslInserted(relatedMessageId.getId());
        }
      } catch (InvalidUnidentifiedAccessHeaderException e) {
        Log.w(TAG, "Someone had a bad UD header. Falling back to legacy sends.", e);
        legacyTargets.addAll(senderKeyTargets);

        if (useGroupSendEndorsements) {
          GroupSendEndorsementInternalNotifier.maybePostGroupSendFallbackError(context);
        }
      } catch (NoSessionException e) {
        Log.w(TAG, "No session. Falling back to legacy sends.", e);
        legacyTargets.addAll(senderKeyTargets);
      } catch (InvalidKeyException e) {
        Log.w(TAG, "Invalid key. Falling back to legacy sends.", e);
        legacyTargets.addAll(senderKeyTargets);
      } catch (InvalidRegistrationIdException e) {
        Log.w(TAG, "Invalid registrationId. Falling back to legacy sends.", e);
        legacyTargets.addAll(senderKeyTargets);
      } catch (NotFoundException e) {
        Log.w(TAG, "Someone was unregistered. Falling back to legacy sends.", e);
        legacyTargets.addAll(senderKeyTargets);
      }
    } else if (relatedMessageId != null) {
      ZonaRosaLocalMetrics.GroupMessageSend.onSenderKeyShared(relatedMessageId.getId());
      ZonaRosaLocalMetrics.GroupMessageSend.onSenderKeyEncrypted(relatedMessageId.getId());
      ZonaRosaLocalMetrics.GroupMessageSend.onSenderKeyMessageSent(relatedMessageId.getId());
      ZonaRosaLocalMetrics.GroupMessageSend.onSenderKeySyncSent(relatedMessageId.getId());
      ZonaRosaLocalMetrics.GroupMessageSend.onSenderKeyMslInserted(relatedMessageId.getId());
    }

    if (cancelationZonaRosa != null && cancelationZonaRosa.isCanceled()) {
      Log.i(TAG, "Send canceled. Adding canceled results for " + legacyTargets.size() + " remaining legacy targets.");
      for (Recipient recipient : legacyTargets) {
        allResults.add(SendMessageResult.canceledFailure(recipients.getAddress(recipient.getId())));
      }

      if (unregisteredTargets.size() > 0) {
        List<SendMessageResult> unregisteredResults = unregisteredTargets.stream()
                                                                         .filter(Recipient::getHasServiceId)
                                                                         .map(t -> SendMessageResult.unregisteredFailure(new ZonaRosaServiceAddress(t.requireServiceId(), t.getE164().orElse(null))))
                                                                         .collect(Collectors.toList());
        allResults.addAll(unregisteredResults);
      }

      return allResults;
    }

    boolean onlyTargetIsSelfWithLinkedDevice = legacyTargets.isEmpty() && senderKeyTargets.isEmpty() && ZonaRosaStore.account().isMultiDevice();

    if (legacyTargets.size() > 0 || onlyTargetIsSelfWithLinkedDevice) {
      if (legacyTargets.size() > 0) {
        Log.i(TAG, "Need to do " + legacyTargets.size() + " legacy sends.");
      } else {
        Log.i(TAG, "Need to do a legacy send to send a sync message for a group of only ourselves.");
      }

      List<ZonaRosaServiceAddress> legacyTargetAddresses = legacyTargets.stream().map(r -> recipients.getAddress(r.getId())).collect(Collectors.toList());
      List<UnidentifiedAccess>   legacyTargetAccesses  = legacyTargets.stream().map(r -> recipients.getAccess(r.getId())).collect(Collectors.toList());
      List<GroupSendFullToken>   groupSendTokens       = null;
      boolean                    recipientUpdate       = isRecipientUpdate || allResults.size() > 0;

      if (useGroupSendEndorsements) {
        Instant           expiration        = Instant.ofEpochMilli(groupSendEndorsementExpiration);
        GroupSecretParams groupSecretParams = GroupSecretParams.deriveFromMasterKey(groupRecord.get().requireV2GroupProperties().getGroupMasterKey());

        groupSendTokens = new ArrayList<>(legacyTargetAddresses.size());

        for (Recipient r : legacyTargets) {
          GroupSendEndorsement endorsement = groupSendEndorsementRecords.getEndorsement(r.getId());
          if (r.getHasAci() && endorsement != null) {
            groupSendTokens.add(endorsement.toFullToken(groupSecretParams, expiration));
          } else {
            groupSendTokens.add(null);
          }
        }
      }

      final MessageSendLogTables messageLogDatabase  = ZonaRosaDatabase.messageLog();
      final AtomicLong           entryId             = new AtomicLong(-1);
      final boolean              includeInMessageLog = sendOperation.shouldIncludeInMessageLog();

      List<SendMessageResult> results = sendOperation.sendLegacy(messageSender, legacyTargetAddresses, legacyTargets, SealedSenderAccess.forFanOutGroupSend(groupSendTokens, SealedSenderAccessUtil.getSealedSenderCertificate(), legacyTargetAccesses), recipientUpdate, result -> {
        if (!includeInMessageLog) {
          return;
        }

        synchronized (entryId) {
          if (entryId.get() == -1) {
            entryId.set(messageLogDatabase.insertIfPossible(recipients.requireRecipientId(result.getAddress()), sendOperation.getSentTimestamp(), result, sendOperation.getContentHint(), sendOperation.getRelatedMessageId(), sendOperation.isUrgent()));
          } else {
            entryId.set(messageLogDatabase.addRecipientToExistingEntryIfPossible(entryId.get(), recipients.requireRecipientId(result.getAddress()), sendOperation.getSentTimestamp(), result, sendOperation.getContentHint(), sendOperation.getRelatedMessageId(), sendOperation.isUrgent()));
          }
        }
      }, cancelationZonaRosa);

      allResults.addAll(results);

      int successCount = (int) results.stream().filter(SendMessageResult::isSuccess).count();
      Log.d(TAG, "Successfully sent using 1:1 to " + successCount + "/" + legacyTargetAddresses.size() + " legacy targets.");
    } else if (relatedMessageId != null) {
      ZonaRosaLocalMetrics.GroupMessageSend.onLegacyMessageSent(relatedMessageId.getId());
      ZonaRosaLocalMetrics.GroupMessageSend.onLegacySyncFinished(relatedMessageId.getId());
    }

    if (unregisteredTargets.size() > 0) {
      Log.w(TAG, "There are " + unregisteredTargets.size() + " unregistered targets. Including failure results.");

      List<SendMessageResult> unregisteredResults = unregisteredTargets.stream()
                                                                       .filter(Recipient::getHasServiceId)
                                                                       .map(t -> SendMessageResult.unregisteredFailure(new ZonaRosaServiceAddress(t.requireServiceId(), t.getE164().orElse(null))))
                                                                       .collect(Collectors.toList());

      if (unregisteredResults.size() < unregisteredTargets.size()) {
        Log.w(TAG, "There are " + (unregisteredTargets.size() - unregisteredResults.size()) + " targets that have no UUID! Cannot report a failure for them.");
      }

      allResults.addAll(unregisteredResults);
    }

    return allResults;
  }

  private static @Nullable DistributionId getDistributionId(@Nullable GroupId.V2 groupId) {
    if (groupId != null) {
      return ZonaRosaDatabase.groups().getOrCreateDistributionId(groupId);
    } else {
      return null;
    }
  }

  private static @Nullable DistributionId getDistributionId(@Nullable DistributionListId distributionListId) {
    if (distributionListId != null) {
      return Optional.ofNullable(ZonaRosaDatabase.distributionLists().getDistributionId(distributionListId)).orElse(null);
    } else {
      return null;
    }
  }

  /** Abstraction layer to handle the different types of message send operations we can do */
  private interface SendOperation {
    @NonNull List<SendMessageResult> sendWithSenderKey(@NonNull ZonaRosaServiceMessageSender messageSender,
                                                       @NonNull DistributionId distributionId,
                                                       @NonNull List<ZonaRosaServiceAddress> targets,
                                                       @NonNull List<UnidentifiedAccess> access,
                                                       @Nullable GroupSendEndorsements groupSendEndorsements,
                                                       boolean isRecipientUpdate,
                                                       @Nullable PartialSendBatchCompleteListener partialListener)
        throws NoSessionException, UntrustedIdentityException, InvalidKeyException, IOException, InvalidRegistrationIdException;

    @NonNull List<SendMessageResult> sendLegacy(@NonNull ZonaRosaServiceMessageSender messageSender,
                                                @NonNull List<ZonaRosaServiceAddress> targets,
                                                @NonNull List<Recipient> targetRecipients,
                                                @NonNull List<SealedSenderAccess> sealedSenderAccesses,
                                                boolean isRecipientUpdate,
                                                @Nullable PartialSendCompleteListener partialListener,
                                                @Nullable CancelationZonaRosa cancelationZonaRosa)
        throws IOException, UntrustedIdentityException;

    @NonNull ContentHint getContentHint();
    long getSentTimestamp();
    boolean shouldIncludeInMessageLog();
    @NonNull MessageId getRelatedMessageId();
    boolean isUrgent();
  }

  private static class DataSendOperation implements SendOperation {
    private final ZonaRosaServiceDataMessage message;
    private final ContentHint              contentHint;
    private final MessageId                relatedMessageId;
    private final boolean                  resendable;
    private final boolean                  urgent;
    private final boolean                  isForStory;
    private final ZonaRosaServiceEditMessage editMessage;

    public static DataSendOperation resendable(@NonNull ZonaRosaServiceDataMessage message, @NonNull ContentHint contentHint, @NonNull MessageId relatedMessageId, boolean urgent, boolean isForStory, @Nullable ZonaRosaServiceEditMessage editMessage) {
      return new DataSendOperation(editMessage != null ? editMessage.getDataMessage() : message, contentHint, true, relatedMessageId, urgent, isForStory, editMessage);
    }

    public static DataSendOperation unresendable(@NonNull ZonaRosaServiceDataMessage message, @NonNull ContentHint contentHint, boolean urgent) {
      return new DataSendOperation(message, contentHint, false, null, urgent, false, null);
    }

    private DataSendOperation(@NonNull ZonaRosaServiceDataMessage message, @NonNull ContentHint contentHint, boolean resendable, @Nullable MessageId relatedMessageId, boolean urgent, boolean isForStory, @Nullable ZonaRosaServiceEditMessage editMessage) {
      this.message          = message;
      this.contentHint      = contentHint;
      this.resendable       = resendable;
      this.relatedMessageId = relatedMessageId;
      this.urgent           = urgent;
      this.isForStory       = isForStory;
      this.editMessage      = editMessage;

      if (resendable && relatedMessageId == null) {
        throw new IllegalArgumentException("If a message is resendable, it must have a related message ID!");
      }
    }

    @Override
    public @NonNull List<SendMessageResult> sendWithSenderKey(@NonNull ZonaRosaServiceMessageSender messageSender,
                                                              @NonNull DistributionId distributionId,
                                                              @NonNull List<ZonaRosaServiceAddress> targets,
                                                              @NonNull List<UnidentifiedAccess> access,
                                                              @Nullable GroupSendEndorsements groupSendEndorsements,
                                                              boolean isRecipientUpdate,
                                                              @Nullable PartialSendBatchCompleteListener partialListener)
        throws NoSessionException, UntrustedIdentityException, InvalidKeyException, IOException, InvalidRegistrationIdException
    {
      SenderKeyGroupEvents listener = relatedMessageId != null ? new SenderKeyMetricEventListener(relatedMessageId.getId()) : SenderKeyGroupEvents.EMPTY;
      return messageSender.sendGroupDataMessage(distributionId, targets, access, groupSendEndorsements, isRecipientUpdate, contentHint, message, listener, urgent, isForStory, editMessage, partialListener);
    }

    @Override
    public @NonNull List<SendMessageResult> sendLegacy(@NonNull ZonaRosaServiceMessageSender messageSender,
                                                       @NonNull List<ZonaRosaServiceAddress> targets,
                                                       @NonNull List<Recipient> targetRecipients,
                                                       @NonNull List<SealedSenderAccess> sealedSenderAccesses,
                                                       boolean isRecipientUpdate,
                                                       @Nullable PartialSendCompleteListener partialListener,
                                                       @Nullable CancelationZonaRosa cancelationZonaRosa)
        throws IOException, UntrustedIdentityException
    {
      // PniSignatures are only needed for 1:1 messages, but some message jobs use the GroupSendUtil methods to send 1:1
      if (targets.size() == 1 && relatedMessageId == null) {
        Recipient          targetRecipient    = targetRecipients.get(0);
        SealedSenderAccess sealedSenderAccess = sealedSenderAccesses.get(0);
        SendMessageResult  result;

        try {
          if (editMessage != null) {
            result = messageSender.sendEditMessage(targets.get(0), sealedSenderAccess, contentHint, message, ZonaRosaServiceMessageSender.IndividualSendEvents.EMPTY, urgent, editMessage.getTargetSentTimestamp());
          } else {
            result = messageSender.sendDataMessage(targets.get(0), sealedSenderAccess, contentHint, message, ZonaRosaServiceMessageSender.IndividualSendEvents.EMPTY, urgent, targetRecipient.getNeedsPniSignature());
          }
        } catch (IOException e) {
          result = ZonaRosaServiceMessageSender.mapSendErrorToSendResult(e, message.getTimestamp(), targets.get(0));
        }

        if (result.isSuccess() && targetRecipient.getNeedsPniSignature()) {
          ZonaRosaDatabase.pendingPniSignatureMessages().insertIfNecessary(targetRecipients.get(0).getId(), getSentTimestamp(), result);
        }

        return Collections.singletonList(result);
      } else {
        LegacyGroupEvents listener = relatedMessageId != null ? new LegacyMetricEventListener(relatedMessageId.getId()) : LegacyGroupEvents.EMPTY;

        if (editMessage != null) {
          return messageSender.sendEditMessage(targets, sealedSenderAccesses, isRecipientUpdate, contentHint, message, listener, partialListener, cancelationZonaRosa, urgent, editMessage.getTargetSentTimestamp());
        } else {
          return messageSender.sendDataMessage(targets, sealedSenderAccesses, isRecipientUpdate, contentHint, message, listener, partialListener, cancelationZonaRosa, urgent);
        }
      }
    }

    @Override
    public @NonNull ContentHint getContentHint() {
      return contentHint;
    }

    @Override
    public long getSentTimestamp() {
      return message.getTimestamp();
    }

    @Override
    public boolean shouldIncludeInMessageLog() {
      return resendable;
    }

    @Override
    public @NonNull MessageId getRelatedMessageId() {
      if (relatedMessageId != null) {
        return relatedMessageId;
      } else {
        throw new UnsupportedOperationException();
      }
    }

    @Override
    public boolean isUrgent() {
      return urgent;
    }
  }

  private static class TypingSendOperation implements SendOperation {

    private final ZonaRosaServiceTypingMessage message;

    private TypingSendOperation(@NonNull ZonaRosaServiceTypingMessage message) {
      this.message = message;
    }

    @Override
    public @NonNull List<SendMessageResult> sendWithSenderKey(@NonNull ZonaRosaServiceMessageSender messageSender,
                                                              @NonNull DistributionId distributionId,
                                                              @NonNull List<ZonaRosaServiceAddress> targets,
                                                              @NonNull List<UnidentifiedAccess> access,
                                                              @Nullable GroupSendEndorsements groupSendEndorsements,
                                                              boolean isRecipientUpdate,
                                                              @Nullable PartialSendBatchCompleteListener partialListener)
        throws NoSessionException, UntrustedIdentityException, InvalidKeyException, IOException, InvalidRegistrationIdException
    {
      Preconditions.checkNotNull(groupSendEndorsements, "GSEs must be non-null for non-story sender key send.");

      messageSender.sendGroupTyping(distributionId, targets, access, groupSendEndorsements, message);
      List<SendMessageResult> results = targets.stream().map(a -> SendMessageResult.success(a, Collections.emptyList(), true, false, -1, Optional.empty())).collect(Collectors.toList());

      if (partialListener != null) {
        partialListener.onPartialSendComplete(results);
      }

      return results;
    }

    @Override
    public @NonNull List<SendMessageResult> sendLegacy(@NonNull ZonaRosaServiceMessageSender messageSender,
                                                       @NonNull List<ZonaRosaServiceAddress> targets,
                                                       @NonNull List<Recipient> targetRecipients,
                                                       @NonNull List<SealedSenderAccess> sealedSenderAccesses,
                                                       boolean isRecipientUpdate,
                                                       @Nullable PartialSendCompleteListener partialListener,
                                                       @Nullable CancelationZonaRosa cancelationZonaRosa)
        throws IOException
    {
      messageSender.sendTyping(targets, sealedSenderAccesses, message, cancelationZonaRosa);
      return targets.stream().map(a -> SendMessageResult.success(a, Collections.emptyList(), true, false, -1, Optional.empty())).collect(Collectors.toList());
    }

    @Override
    public @NonNull ContentHint getContentHint() {
      return ContentHint.IMPLICIT;
    }

    @Override
    public long getSentTimestamp() {
      return message.getTimestamp();
    }

    @Override
    public boolean shouldIncludeInMessageLog() {
      return false;
    }

    @Override
    public @NonNull MessageId getRelatedMessageId() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isUrgent() {
      return false;
    }
  }

  private static class CallSendOperation implements SendOperation {

    private final ZonaRosaServiceCallMessage message;

    private CallSendOperation(@NonNull ZonaRosaServiceCallMessage message) {
      this.message = message;
    }

    @Override
    public @NonNull List<SendMessageResult> sendWithSenderKey(@NonNull ZonaRosaServiceMessageSender messageSender,
                                                              @NonNull DistributionId distributionId,
                                                              @NonNull List<ZonaRosaServiceAddress> targets,
                                                              @NonNull List<UnidentifiedAccess> access,
                                                              @Nullable GroupSendEndorsements groupSendEndorsements,
                                                              boolean isRecipientUpdate,
                                                              @Nullable PartialSendBatchCompleteListener partialSendListener)
        throws NoSessionException, UntrustedIdentityException, InvalidKeyException, IOException, InvalidRegistrationIdException
    {
      Preconditions.checkNotNull(groupSendEndorsements, "GSEs must be non-null for non-story sender key send.");

      return messageSender.sendCallMessage(distributionId, targets, access, groupSendEndorsements, message, partialSendListener);
    }

    @Override
    public @NonNull List<SendMessageResult> sendLegacy(@NonNull ZonaRosaServiceMessageSender messageSender,
                                                       @NonNull List<ZonaRosaServiceAddress> targets,
                                                       @NonNull List<Recipient> targetRecipients,
                                                       @NonNull List<SealedSenderAccess> sealedSenderAccesses,
                                                       boolean isRecipientUpdate,
                                                       @Nullable PartialSendCompleteListener partialListener,
                                                       @Nullable CancelationZonaRosa cancelationZonaRosa)
        throws IOException
    {
      return messageSender.sendCallMessage(targets, sealedSenderAccesses, message);
    }

    @Override
    public @NonNull ContentHint getContentHint() {
      return ContentHint.IMPLICIT;
    }

    @Override
    public long getSentTimestamp() {
      return message.getTimestamp().get();
    }

    @Override
    public boolean shouldIncludeInMessageLog() {
      return false;
    }

    @Override
    public @NonNull MessageId getRelatedMessageId() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isUrgent() {
      return message.isUrgent();
    }
  }

  public static class StorySendOperation implements SendOperation {

    private final MessageId                               relatedMessageId;
    private final GroupId                                 groupId;
    private final long                                    sentTimestamp;
    private final ZonaRosaServiceStoryMessage               message;
    private final Set<ZonaRosaServiceStoryMessageRecipient> manifest;

    public StorySendOperation(@NonNull MessageId relatedMessageId,
                              @Nullable GroupId groupId,
                              long sentTimestamp,
                              @NonNull ZonaRosaServiceStoryMessage message,
                              @NonNull Set<ZonaRosaServiceStoryMessageRecipient> manifest)
    {
      this.relatedMessageId = relatedMessageId;
      this.groupId          = groupId;
      this.sentTimestamp    = sentTimestamp;
      this.message          = message;
      this.manifest         = manifest;
    }

    @Override
    public @NonNull List<SendMessageResult> sendWithSenderKey(@NonNull ZonaRosaServiceMessageSender messageSender,
                                                              @NonNull DistributionId distributionId,
                                                              @NonNull List<ZonaRosaServiceAddress> targets,
                                                              @NonNull List<UnidentifiedAccess> access,
                                                              @Nullable GroupSendEndorsements groupSendEndorsements,
                                                              boolean isRecipientUpdate,
                                                              @Nullable PartialSendBatchCompleteListener partialListener)
        throws NoSessionException, UntrustedIdentityException, InvalidKeyException, IOException, InvalidRegistrationIdException
    {
      return messageSender.sendGroupStory(distributionId, Optional.ofNullable(groupId).map(GroupId::getDecodedId), targets, access, groupSendEndorsements, isRecipientUpdate, message, getSentTimestamp(), manifest, partialListener);
    }

    @Override
    public @NonNull List<SendMessageResult> sendLegacy(@NonNull ZonaRosaServiceMessageSender messageSender,
                                                       @NonNull List<ZonaRosaServiceAddress> targets,
                                                       @NonNull List<Recipient> targetRecipients,
                                                       @NonNull List<SealedSenderAccess> sealedSenderAccesses,
                                                       boolean isRecipientUpdate,
                                                       @Nullable PartialSendCompleteListener partialListener,
                                                       @Nullable CancelationZonaRosa cancelationZonaRosa)
        throws IOException, UntrustedIdentityException
    {
      // We only allow legacy sends if you're sending to an empty group and just need to send a sync message.
      if (targets.isEmpty()) {
        Log.w(TAG, "Only sending a sync message.");
        messageSender.sendStorySyncMessage(message, getSentTimestamp(), isRecipientUpdate, manifest);
        return Collections.emptyList();
      } else {
        throw new UnsupportedOperationException("Stories can only be send via sender key!");
      }
    }

    @Override
    public @NonNull ContentHint getContentHint() {
      return ContentHint.IMPLICIT;
    }

    @Override
    public long getSentTimestamp() {
      return sentTimestamp;
    }

    @Override
    public boolean shouldIncludeInMessageLog() {
      return true;
    }

    @Override
    public @NonNull MessageId getRelatedMessageId() {
      return relatedMessageId;
    }

    @Override
    public boolean isUrgent() {
      return false;
    }
  }

  private static final class SenderKeyMetricEventListener implements SenderKeyGroupEvents {

    private final long messageId;

    private SenderKeyMetricEventListener(long messageId) {
      this.messageId = messageId;
    }

    @Override
    public void onSenderKeyShared() {
      ZonaRosaLocalMetrics.GroupMessageSend.onSenderKeyShared(messageId);
    }

    @Override
    public void onMessageEncrypted() {
      ZonaRosaLocalMetrics.GroupMessageSend.onSenderKeyEncrypted(messageId);
    }

    @Override
    public void onMessageSent() {
      ZonaRosaLocalMetrics.GroupMessageSend.onSenderKeyMessageSent(messageId);
    }

    @Override
    public void onSyncMessageSent() {
      ZonaRosaLocalMetrics.GroupMessageSend.onSenderKeySyncSent(messageId);
    }
  }

  private static final class LegacyMetricEventListener implements LegacyGroupEvents {

    private final long messageId;

    private LegacyMetricEventListener(long messageId) {
      this.messageId = messageId;
    }

    @Override
    public void onMessageEncrypted() {}

    @Override
    public void onMessageSent() {
      ZonaRosaLocalMetrics.GroupMessageSend.onLegacyMessageSent(messageId);
    }

    @Override
    public void onSyncMessageSent() {
      ZonaRosaLocalMetrics.GroupMessageSend.onLegacySyncFinished(messageId);
    }
  }

  /**
   * Little utility wrapper that lets us get the various different slices of recipient models that we need for different methods.
   */
  private static final class RecipientData {

    private final Map<RecipientId, Optional<UnidentifiedAccess>> accessById;
    private final Map<RecipientId, ZonaRosaServiceAddress>             addressById;
    private final RecipientAccessList                                accessList;

    RecipientData(@NonNull Context context, @NonNull List<Recipient> recipients, boolean isForStory) throws IOException {
      this.accessById  = SealedSenderAccessUtil.getAccessMapFor(recipients, isForStory);
      this.addressById = mapAddresses(context, recipients);
      this.accessList  = new RecipientAccessList(recipients);
    }

    @NonNull ZonaRosaServiceAddress getAddress(@NonNull RecipientId id) {
      return Objects.requireNonNull(addressById.get(id));
    }

    @NonNull Optional<UnidentifiedAccess> getAccessPair(@NonNull RecipientId id) {
      return Objects.requireNonNull(accessById.get(id));
    }

    @Nullable UnidentifiedAccess getAccess(@NonNull RecipientId id) {
      return Objects.requireNonNull(accessById.get(id)).orElse(null);
    }

    @NonNull UnidentifiedAccess requireAccess(@NonNull RecipientId id) {
      return Objects.requireNonNull(accessById.get(id)).get();
    }

    @NonNull RecipientId requireRecipientId(@NonNull ZonaRosaServiceAddress address) {
      return accessList.requireIdByAddress(address);
    }

    private static @NonNull Map<RecipientId, ZonaRosaServiceAddress> mapAddresses(@NonNull Context context, @NonNull List<Recipient> recipients) throws IOException {
      List<ZonaRosaServiceAddress> addresses = RecipientUtil.toZonaRosaServiceAddressesFromResolved(context, recipients);

      Iterator<Recipient>            recipientIterator = recipients.iterator();
      Iterator<ZonaRosaServiceAddress> addressIterator   = addresses.iterator();

      Map<RecipientId, ZonaRosaServiceAddress> map = new HashMap<>(recipients.size());

      while (recipientIterator.hasNext()) {
        map.put(recipientIterator.next().getId(), addressIterator.next());
      }

      return map;
    }
  }
}
