package io.zonarosa.messenger.recipients;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.annimon.stream.Stream;

import io.zonarosa.core.util.logging.Log;
import io.zonarosa.messenger.contacts.sync.ContactDiscovery;
import io.zonarosa.messenger.database.GroupTable;
import io.zonarosa.messenger.database.RecipientTable.RegisteredState;
import io.zonarosa.messenger.database.ZonaRosaDatabase;
import io.zonarosa.messenger.database.ThreadTable;
import io.zonarosa.messenger.database.model.GroupRecord;
import io.zonarosa.messenger.dependencies.AppDependencies;
import io.zonarosa.messenger.groups.GroupChangeBusyException;
import io.zonarosa.messenger.groups.GroupChangeException;
import io.zonarosa.messenger.groups.GroupChangeFailedException;
import io.zonarosa.messenger.groups.GroupManager;
import io.zonarosa.messenger.jobs.MultiDeviceBlockedUpdateJob;
import io.zonarosa.messenger.jobs.RefreshOwnProfileJob;
import io.zonarosa.messenger.jobs.RotateProfileKeyJob;
import io.zonarosa.messenger.keyvalue.ZonaRosaStore;
import io.zonarosa.messenger.mms.MmsException;
import io.zonarosa.messenger.mms.OutgoingMessage;
import io.zonarosa.messenger.sms.MessageSender;
import io.zonarosa.messenger.storage.StorageSyncHelper;
import io.zonarosa.core.models.ServiceId;
import io.zonarosa.service.api.push.ZonaRosaServiceAddress;
import io.zonarosa.service.api.push.exceptions.NotFoundException;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class RecipientUtil {

  private static final String TAG = Log.tag(RecipientUtil.class);

  /**
   * This method will do it's best to get a {@link ServiceId} for the provided recipient. This includes performing
   * a possible network request if no ServiceId is available. If the request to get a ServiceId fails or the user is
   * not registered, an IOException is thrown.
   */
  @WorkerThread
  public static @NonNull ServiceId getOrFetchServiceId(@NonNull Context context, @NonNull Recipient recipient) throws IOException {
    return toZonaRosaServiceAddress(context, recipient).getServiceId();
  }

  /**
   * This method will do it's best to craft a fully-populated {@link ZonaRosaServiceAddress} based on
   * the provided recipient. This includes performing a possible network request if no UUID is
   * available. If the request to get a UUID fails or the user is not registered, an IOException is thrown.
   */
  @WorkerThread
  public static @NonNull ZonaRosaServiceAddress toZonaRosaServiceAddress(@NonNull Context context, @NonNull Recipient recipient)
      throws IOException
  {
    recipient = recipient.resolve();

    if (!recipient.getServiceId().isPresent() && !recipient.getE164().isPresent()) {
      throw new AssertionError(recipient.getId() + " - No UUID or phone number!");
    }

    if (!recipient.getServiceId().isPresent()) {
      Log.i(TAG, recipient.getId() + " is missing a UUID...");
      RegisteredState state = ContactDiscovery.refresh(context, recipient, false);

      recipient = Recipient.resolved(recipient.getId());
      Log.i(TAG, "Successfully performed a UUID fetch for " + recipient.getId() + ". Registered: " + state);
    }

    if (recipient.getHasServiceId()) {
      return new ZonaRosaServiceAddress(recipient.requireServiceId(), Optional.ofNullable(recipient.resolve().getE164().orElse(null)));
    } else {
      throw new NotFoundException(recipient.getId() + " is not registered!");
    }
  }

  public static @NonNull List<ZonaRosaServiceAddress> toZonaRosaServiceAddressesFromResolved(@NonNull Context context, @NonNull List<Recipient> recipients)
      throws IOException
  {
    ensureUuidsAreAvailable(context, recipients);

    List<Recipient> latestRecipients = recipients.stream().map(it -> it.live().resolve()).collect(Collectors.toList());

    if (latestRecipients.stream().anyMatch(it -> !it.getHasServiceId())) {
      throw new NotFoundException("1 or more recipients are not registered!");
    }

    return latestRecipients
        .stream()
        .map(r -> new ZonaRosaServiceAddress(r.requireServiceId(), r.getE164().orElse(null)))
        .collect(Collectors.toList());
  }

  /**
   * Ensures that UUIDs are available. If a UUID cannot be retrieved or a user is found to be unregistered, an exception is thrown.
   */
  public static boolean ensureUuidsAreAvailable(@NonNull Context context, @NonNull Collection<Recipient> recipients)
      throws IOException
  {
    List<Recipient> recipientsWithoutUuids = Stream.of(recipients)
                                                   .map(Recipient::resolve)
                                                   .filterNot(Recipient::getHasServiceId)
                                                   .toList();

    if (recipientsWithoutUuids.size() > 0) {
      ContactDiscovery.refresh(context, recipientsWithoutUuids, false);

      if (recipients.stream().map(Recipient::resolve).anyMatch(it -> it.isUnregistered() || !it.getHasServiceId())) {
        throw new NotFoundException("1 or more recipients are not registered!");
      }

      return true;
    } else {
      return false;
    }
  }

  public static boolean isBlockable(@NonNull Recipient recipient) {
    Recipient resolved = recipient.resolve();
    return !resolved.isMmsGroup();
  }

  public static List<Recipient> getEligibleForSending(@NonNull List<Recipient> recipients) {
    return Stream.of(recipients)
                 .filter(r -> r.getRegistered() != RegisteredState.NOT_REGISTERED)
                 .filter(r -> !r.isBlocked())
                 .toList();
  }

  /**
   * You can call this for non-groups and not have to handle any network errors.
   */
  @WorkerThread
  public static void blockNonGroup(@NonNull Context context, @NonNull Recipient recipient) {
    if (recipient.isGroup()) {
      throw new AssertionError();
    }

    try {
      block(context, recipient);
    } catch (GroupChangeException | IOException e) {
      throw new AssertionError(e);
    }
  }

  /**
   * You can call this for any type of recipient but must handle network errors that can occur from
   * GV2.
   * <p>
   * GV2 operations can also take longer due to the network.
   */
  @WorkerThread
  public static void block(@NonNull Context context, @NonNull Recipient recipient)
      throws GroupChangeBusyException, IOException, GroupChangeFailedException
  {
    if (!isBlockable(recipient)) {
      throw new AssertionError("Recipient is not blockable!");
    }
    Log.w(TAG, "Blocking " + recipient.getId() + " (group: " + recipient.isGroup() + ")");

    recipient = recipient.resolve();

    if (recipient.isGroup() && recipient.getGroupId().get().isPush()) {
      GroupManager.leaveGroupFromBlockOrMessageRequest(context, recipient.getGroupId().get().requirePush());
    }

    ZonaRosaDatabase.recipients().setBlocked(recipient.getId(), true);
    insertBlockedUpdate(recipient, ZonaRosaDatabase.threads().getOrCreateThreadIdFor(recipient));

    RecipientUtil.updateProfileSharingAfterBlock(recipient, true);

    AppDependencies.getJobManager().add(new MultiDeviceBlockedUpdateJob());
    StorageSyncHelper.scheduleSyncForDataChange();
  }

  @WorkerThread
  public static boolean updateProfileSharingAfterBlock(@NonNull Recipient recipient, boolean rotateProfileKeyOnBlock) {
    if (recipient.isSystemContact() || recipient.isProfileSharing() || isProfileSharedViaGroup(recipient)) {
      ZonaRosaDatabase.recipients().setProfileSharing(recipient.getId(), false);

      if (rotateProfileKeyOnBlock) {
        Log.i(TAG, "Rotating profile key");
        AppDependencies.getJobManager().startChain(new RefreshOwnProfileJob())
                       .then(new RotateProfileKeyJob())
                       .enqueue();

        return true;
      }
    }

    return false;
  }

  @WorkerThread
  public static void unblock(@NonNull Recipient recipient) {
    if (!isBlockable(recipient)) {
      throw new AssertionError("Recipient is not blockable!");
    }
    Log.i(TAG, "Unblocking " + recipient.getId() + " (group: " + recipient.isGroup() + ")", new Throwable());

    ZonaRosaDatabase.recipients().setBlocked(recipient.getId(), false);
    ZonaRosaDatabase.recipients().setProfileSharing(recipient.getId(), true);
    insertUnblockedUpdate(recipient, ZonaRosaDatabase.threads().getOrCreateThreadIdFor(recipient));
    AppDependencies.getJobManager().add(new MultiDeviceBlockedUpdateJob());
    StorageSyncHelper.scheduleSyncForDataChange();
  }

  private static void insertBlockedUpdate(@NonNull Recipient recipient, long threadId) {
    try {
      ZonaRosaDatabase.messages().insertMessageOutbox(
        OutgoingMessage.blockedMessage(recipient, System.currentTimeMillis(), TimeUnit.SECONDS.toMillis(recipient.getExpiresInSeconds())),
        threadId,
        false,
        null
      );
    } catch (MmsException e) {
      Log.w(TAG, "Unable to insert blocked message", e);
    }
  }

  private static void insertUnblockedUpdate(@NonNull Recipient recipient, long threadId) {
    try {
      ZonaRosaDatabase.messages().insertMessageOutbox(
        OutgoingMessage.unblockedMessage(recipient, System.currentTimeMillis(), TimeUnit.SECONDS.toMillis(recipient.getExpiresInSeconds())),
        threadId,
        false,
        null
      );
    } catch (MmsException e) {
      Log.w(TAG, "Unable to insert unblocked message", e);
    }
  }

  @WorkerThread
  public static Recipient.HiddenState getRecipientHiddenState(long threadId) {
    if (threadId < 0) {
      return Recipient.HiddenState.NOT_HIDDEN;
    }

    ThreadTable threadTable     = ZonaRosaDatabase.threads();
    Recipient   threadRecipient = threadTable.getRecipientForThreadId(threadId);

    if (threadRecipient == null) {
      return Recipient.HiddenState.NOT_HIDDEN;
    }

    return threadRecipient.getHiddenState();
  }

  @WorkerThread
  public static boolean isRecipientHidden(long threadId) {
    if (threadId < 0) {
      return false;
    }

    ThreadTable threadTable     = ZonaRosaDatabase.threads();
    Recipient   threadRecipient = threadTable.getRecipientForThreadId(threadId);

    if (threadRecipient == null) {
      return false;
    }

    return threadRecipient.isHidden();
  }

  /**
   * If true, the new message request UI does not need to be shown, and it's safe to send read
   * receipts.
   *
   * Note that this does not imply that a user has explicitly accepted a message request -- it could
   * also be the case that the thread in question is for a system contact or something of the like.
   */
  @WorkerThread
  public static boolean isMessageRequestAccepted(@NonNull Context context, long threadId) {
    if (threadId < 0) {
      return true;
    }

    ThreadTable threadTable     = ZonaRosaDatabase.threads();
    Recipient   threadRecipient = threadTable.getRecipientForThreadId(threadId);

    if (threadRecipient == null) {
      return true;
    }

    return isMessageRequestAccepted(threadId, threadRecipient);
  }

  /**
   * See {@link #isMessageRequestAccepted(Context, long)}.
   */
  @WorkerThread
  public static boolean isMessageRequestAccepted(@NonNull Context context, @Nullable Recipient threadRecipient) {
    if (threadRecipient == null) {
      return true;
    }

    Long threadId = ZonaRosaDatabase.threads().getThreadIdFor(threadRecipient.getId());
    return isMessageRequestAccepted(threadId, threadRecipient);
  }

  /**
   * Like {@link #isMessageRequestAccepted(Context, long)} but with fewer checks around messages so it
   * is more likely to return false.
   */
  @WorkerThread
  public static boolean isCallRequestAccepted(@Nullable Recipient threadRecipient) {
    if (threadRecipient == null) {
      return true;
    }

    Long threadId = ZonaRosaDatabase.threads().getThreadIdFor(threadRecipient.getId());
    return isCallRequestAccepted(threadId, threadRecipient);
  }

  @WorkerThread
  public static void shareProfileIfFirstSecureMessage(@NonNull Recipient recipient) {
    if (recipient.isProfileSharing()) {
      return;
    }

    long    threadId     = ZonaRosaDatabase.threads().getThreadIdIfExistsFor(recipient.getId());
    boolean firstMessage = ZonaRosaDatabase.messages().getOutgoingSecureMessageCount(threadId) == 0;

    if (firstMessage || recipient.isHidden()) {
      ZonaRosaDatabase.recipients().setProfileSharing(recipient.getId(), true);
    }
  }

  public static boolean isLegacyProfileSharingAccepted(@NonNull Recipient threadRecipient) {
    return threadRecipient.isSelf()           ||
           threadRecipient.isProfileSharing() ||
           threadRecipient.isSystemContact()  ||
           !threadRecipient.isRegistered()    ||
           threadRecipient.isHidden();
  }

  /**
   * @return True if this recipient should already have your profile key, otherwise false.
   */
  public static boolean shouldHaveProfileKey(@NonNull Recipient recipient) {
    if (recipient.isBlocked()) {
      return false;
    }

    if (recipient.isProfileSharing()) {
      return true;
    } else {
      GroupTable groupDatabase = ZonaRosaDatabase.groups();
      return groupDatabase.getPushGroupsContainingMember(recipient.getId())
                          .stream()
                          .filter(GroupRecord::isV2Group)
                          .anyMatch(group -> group.memberLevel(Recipient.self()).isInGroup());

    }
  }

  /**
   * Checks if a universal timer is set and if the thread should have it set on it. Attempts to abort quickly and perform
   * minimal database access.
   *
   * @return The new expire timer version if the timer was set, otherwise null.
   */
  @WorkerThread
  public static @Nullable Integer setAndSendUniversalExpireTimerIfNecessary(@NonNull Context context, @NonNull Recipient recipient, long threadId) {
    int defaultTimer = ZonaRosaStore.settings().getUniversalExpireTimer();
    if (defaultTimer == 0 || recipient.isGroup() || recipient.isDistributionList() || recipient.getExpiresInSeconds() != 0 || !recipient.isRegistered()) {
      return null;
    }

    if (threadId == -1 || ZonaRosaDatabase.messages().canSetUniversalTimer(threadId)) {
      int expireTimerVersion = ZonaRosaDatabase.recipients().setExpireMessagesAndIncrementVersion(recipient.getId(), defaultTimer);
      OutgoingMessage outgoingMessage = OutgoingMessage.expirationUpdateMessage(recipient, System.currentTimeMillis(), defaultTimer * 1000L, expireTimerVersion);
      MessageSender.send(context, outgoingMessage, ZonaRosaDatabase.threads().getOrCreateThreadIdFor(recipient), MessageSender.SendType.ZONAROSA, null, null);
      return expireTimerVersion;
    }
    return null;
  }

  @WorkerThread
  public static boolean isMessageRequestAccepted(@Nullable Long threadId, @Nullable Recipient threadRecipient) {
    return threadRecipient == null ||
           threadRecipient.isSelf() ||
           threadRecipient.isProfileSharing() ||
           threadRecipient.isSystemContact() ||
           !threadRecipient.isRegistered() ||
           (!threadRecipient.isHidden() && (
               hasSentMessageInThread(threadId) ||
               noSecureMessagesAndNoCallsInThread(threadId))
           );
  }

  @WorkerThread
  private static boolean isCallRequestAccepted(@Nullable Long threadId, @NonNull Recipient threadRecipient) {
    return threadRecipient.isProfileSharing() ||
           threadRecipient.isSystemContact() ||
           hasSentMessageInThread(threadId);
  }

  @WorkerThread
  public static boolean hasSentMessageInThread(@Nullable Long threadId) {
    return threadId != null && ZonaRosaDatabase.messages().getOutgoingSecureMessageCount(threadId) != 0;
  }

  public static boolean isSmsOnly(long threadId, @NonNull Recipient threadRecipient) {
    return !threadRecipient.isRegistered() ||
           noSecureMessagesAndNoCallsInThread(threadId);
  }

  @WorkerThread
  private static boolean noSecureMessagesAndNoCallsInThread(@Nullable Long threadId) {
    if (threadId == null) {
      return true;
    }

    return ZonaRosaDatabase.messages().getSecureMessageCount(threadId) == 0 &&
           !ZonaRosaDatabase.threads().hasReceivedAnyCallsSince(threadId, 0);
  }

  @WorkerThread
  private static boolean isProfileSharedViaGroup(@NonNull Recipient recipient) {
    return Stream.of(ZonaRosaDatabase.groups().getPushGroupsContainingMember(recipient.getId()))
                 .anyMatch(group -> Recipient.resolved(group.getRecipientId()).isProfileSharing());
  }
}
