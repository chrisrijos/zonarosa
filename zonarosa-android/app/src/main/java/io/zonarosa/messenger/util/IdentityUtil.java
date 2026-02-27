package io.zonarosa.messenger.util;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import io.zonarosa.core.util.concurrent.ListenableFuture;
import io.zonarosa.core.util.concurrent.SettableFuture;
import io.zonarosa.core.util.concurrent.ZonaRosaExecutors;
import io.zonarosa.core.util.concurrent.SimpleTask;
import io.zonarosa.core.util.logging.Log;
import io.zonarosa.libzonarosa.protocol.IdentityKey;
import io.zonarosa.libzonarosa.protocol.InvalidKeyException;
import io.zonarosa.libzonarosa.protocol.ZonaRosaProtocolAddress;
import io.zonarosa.libzonarosa.protocol.state.IdentityKeyStore;
import io.zonarosa.libzonarosa.protocol.state.SessionRecord;
import io.zonarosa.libzonarosa.protocol.state.SessionStore;
import io.zonarosa.messenger.R;
import io.zonarosa.messenger.crypto.ReentrantSessionLock;
import io.zonarosa.messenger.crypto.storage.ZonaRosaIdentityKeyStore;
import io.zonarosa.messenger.database.GroupTable;
import io.zonarosa.messenger.database.IdentityTable;
import io.zonarosa.messenger.database.MessageTable;
import io.zonarosa.messenger.database.MessageTable.InsertResult;
import io.zonarosa.messenger.database.ZonaRosaDatabase;
import io.zonarosa.messenger.database.model.GroupRecord;
import io.zonarosa.messenger.database.model.IdentityRecord;
import io.zonarosa.messenger.dependencies.AppDependencies;
import io.zonarosa.messenger.mms.IncomingMessage;
import io.zonarosa.messenger.mms.MmsException;
import io.zonarosa.messenger.mms.OutgoingMessage;
import io.zonarosa.messenger.notifications.v2.ConversationId;
import io.zonarosa.messenger.recipients.Recipient;
import io.zonarosa.messenger.recipients.RecipientId;
import io.zonarosa.service.api.ZonaRosaSessionLock;
import io.zonarosa.service.api.messages.multidevice.VerifiedMessage;
import io.zonarosa.core.models.ServiceId;
import io.zonarosa.service.api.push.ZonaRosaServiceAddress;
import io.zonarosa.service.internal.push.Verified;

import java.util.List;
import java.util.Optional;

public final class IdentityUtil {

  private IdentityUtil() {}

  private static final String TAG = Log.tag(IdentityUtil.class);

  public static ListenableFuture<Optional<IdentityRecord>> getRemoteIdentityKey(final Context context, final Recipient recipient) {
    final SettableFuture<Optional<IdentityRecord>> future      = new SettableFuture<>();
    final RecipientId                              recipientId = recipient.getId();

    SimpleTask.run(ZonaRosaExecutors.BOUNDED,
                   () -> AppDependencies.getProtocolStore().aci().identities().getIdentityRecord(recipientId),
                   future::set);

    return future;
  }

  public static void markIdentityVerified(Context context, Recipient recipient, boolean verified, boolean remote)
  {
    long         time          = System.currentTimeMillis();
    MessageTable smsDatabase   = ZonaRosaDatabase.messages();
    GroupTable   groupDatabase = ZonaRosaDatabase.groups();

    try (GroupTable.Reader reader = groupDatabase.getGroups()) {

      GroupRecord groupRecord;

      while ((groupRecord = reader.getNext()) != null) {
        if (groupRecord.getMembers().contains(recipient.getId()) && groupRecord.isActive() && !groupRecord.isMms()) {

          if (remote) {
            IncomingMessage incoming = verified ? IncomingMessage.identityVerified(recipient.getId(), time, groupRecord.getId())
                                                : IncomingMessage.identityDefault(recipient.getId(), time, groupRecord.getId());

            try {
              smsDatabase.insertMessageInbox(incoming);
            } catch (MmsException e) {
              throw new AssertionError(e);
            }
          } else {
            RecipientId recipientId    = ZonaRosaDatabase.recipients().getOrInsertFromGroupId(groupRecord.getId());
            Recipient   groupRecipient = Recipient.resolved(recipientId);
            long        threadId       = ZonaRosaDatabase.threads().getOrCreateThreadIdFor(groupRecipient);

            OutgoingMessage outgoing;
            if (verified) {
              outgoing = OutgoingMessage.identityVerifiedMessage(recipient, time);
            } else {
              outgoing = OutgoingMessage.identityDefaultMessage(recipient, time);
            }

            try {
              ZonaRosaDatabase.messages().insertMessageOutbox(outgoing, threadId, false, null);
            } catch (MmsException e) {
              throw new AssertionError(e);
            }
            ZonaRosaDatabase.threads().update(threadId, true, true);
          }
        }
      }
    }

    if (remote) {
      IncomingMessage incoming = verified ? IncomingMessage.identityVerified(recipient.getId(), time, null)
                                          : IncomingMessage.identityDefault(recipient.getId(), time, null);

      try {
        smsDatabase.insertMessageInbox(incoming);
      } catch (MmsException e) {
        throw new AssertionError(e);
      }
    } else {
      OutgoingMessage outgoing;
      if (verified) {
        outgoing = OutgoingMessage.identityVerifiedMessage(recipient, time);
      } else {
        outgoing = OutgoingMessage.identityDefaultMessage(recipient, time);
      }

      long threadId = ZonaRosaDatabase.threads().getOrCreateThreadIdFor(recipient);

      Log.i(TAG, "Inserting verified outbox...");
      try {
        ZonaRosaDatabase.messages().insertMessageOutbox(outgoing, threadId, false, null);
      } catch (MmsException e) {
        throw new AssertionError();
      }
      ZonaRosaDatabase.threads().update(threadId, true, true);
    }
  }

  public static void markIdentityUpdate(@NonNull Context context, @NonNull RecipientId recipientId) {
    Log.w(TAG, "Inserting safety number change event(s) for " + recipientId, new Throwable());

    long         time          = System.currentTimeMillis();
    MessageTable smsDatabase   = ZonaRosaDatabase.messages();
    GroupTable   groupDatabase = ZonaRosaDatabase.groups();

    try (GroupTable.Reader reader = groupDatabase.getGroups()) {
      GroupRecord groupRecord;

      while ((groupRecord = reader.getNext()) != null) {
        if (groupRecord.getMembers().contains(recipientId) && groupRecord.isActive()) {
          IncomingMessage groupUpdate = IncomingMessage.identityUpdate(recipientId, time, groupRecord.getId());
          smsDatabase.insertMessageInbox(groupUpdate);
        }
      }
    } catch (MmsException e) {
      throw new AssertionError(e);
    }

    try {
      IncomingMessage        individualUpdate = IncomingMessage.identityUpdate(recipientId, time, null);
      Optional<InsertResult> insertResult     = smsDatabase.insertMessageInbox(individualUpdate);

      if (insertResult.isPresent()) {
        AppDependencies.getMessageNotifier().updateNotification(context, ConversationId.forConversation(insertResult.get().getThreadId()));
      }
    } catch (MmsException e) {
      throw new AssertionError(e);
    }

    ZonaRosaDatabase.messageLog().deleteAllForRecipient(recipientId);
  }

  public static void saveIdentity(String user, IdentityKey identityKey) {
    try(ZonaRosaSessionLock.Lock unused = ReentrantSessionLock.INSTANCE.acquire()) {
      SessionStore          sessionStore     = AppDependencies.getProtocolStore().aci();
      ZonaRosaProtocolAddress address          = new ZonaRosaProtocolAddress(user, ZonaRosaServiceAddress.DEFAULT_DEVICE_ID);

      if (AppDependencies.getProtocolStore().aci().identities().saveIdentity(address, identityKey) == IdentityKeyStore.IdentityChange.REPLACED_EXISTING) {
        if (sessionStore.containsSession(address)) {
          SessionRecord sessionRecord = sessionStore.loadSession(address);
          sessionRecord.archiveCurrentState();

          sessionStore.storeSession(address, sessionRecord);
        }
      }
    }
  }

  public static void processVerifiedMessage(Context context, Verified verified) throws InvalidKeyException {
    ZonaRosaServiceAddress          destination = new ZonaRosaServiceAddress(ServiceId.ACI.parseOrThrow(verified.destinationAci, verified.destinationAciBinary));
    IdentityKey                   identityKey = new IdentityKey(verified.identityKey.toByteArray(), 0);
    VerifiedMessage.VerifiedState state;

    switch (verified.state) {
      case DEFAULT:
        state = VerifiedMessage.VerifiedState.DEFAULT;
        break;
      case VERIFIED:
        state = VerifiedMessage.VerifiedState.VERIFIED;
        break;
      case UNVERIFIED:
        state = VerifiedMessage.VerifiedState.UNVERIFIED;
        break;
      default:
        throw new IllegalArgumentException();
    }

    processVerifiedMessage(context, new VerifiedMessage(destination, identityKey, state, System.currentTimeMillis()));
  }

  public static void processVerifiedMessage(Context context, VerifiedMessage verifiedMessage) {
    try(ZonaRosaSessionLock.Lock unused = ReentrantSessionLock.INSTANCE.acquire()) {
      ZonaRosaIdentityKeyStore   identityStore  = AppDependencies.getProtocolStore().aci().identities();
      Recipient                recipient      = Recipient.externalPush(verifiedMessage.getDestination());

      if (recipient.isSelf()) {
        Log.w(TAG, "Attempting to change verified status of self to " + verifiedMessage.getVerified() + ", skipping.");
        return;
      }

      Optional<IdentityRecord> identityRecord = identityStore.getIdentityRecord(recipient.getId());

      if (!identityRecord.isPresent() && verifiedMessage.getVerified() == VerifiedMessage.VerifiedState.DEFAULT) {
        Log.w(TAG, "No existing record for default status");
        return;
      }

      if (verifiedMessage.getVerified() == VerifiedMessage.VerifiedState.DEFAULT              &&
          identityRecord.isPresent()                                                          &&
          identityRecord.get().getIdentityKey().equals(verifiedMessage.getIdentityKey())      &&
          identityRecord.get().getVerifiedStatus() != IdentityTable.VerifiedStatus.DEFAULT)
      {
        Log.i(TAG, "Setting " + recipient.getId() + " verified status to " + IdentityTable.VerifiedStatus.DEFAULT);
        identityStore.setVerified(recipient.getId(), identityRecord.get().getIdentityKey(), IdentityTable.VerifiedStatus.DEFAULT);
        markIdentityVerified(context, recipient, false, true);
      }

      if (verifiedMessage.getVerified() == VerifiedMessage.VerifiedState.VERIFIED &&
          (!identityRecord.isPresent() ||
              (identityRecord.isPresent() && !identityRecord.get().getIdentityKey().equals(verifiedMessage.getIdentityKey())) ||
              (identityRecord.isPresent() && identityRecord.get().getVerifiedStatus() != IdentityTable.VerifiedStatus.VERIFIED)))
      {
        Log.i(TAG, "Setting " + recipient.getId() + " verified status to " + IdentityTable.VerifiedStatus.VERIFIED);
        saveIdentity(verifiedMessage.getDestination().getIdentifier(), verifiedMessage.getIdentityKey());
        identityStore.setVerified(recipient.getId(), verifiedMessage.getIdentityKey(), IdentityTable.VerifiedStatus.VERIFIED);
        markIdentityVerified(context, recipient, true, true);
      }
    }
  }


  public static @Nullable String getUnverifiedBannerDescription(@NonNull Context context,
                                                                @NonNull List<Recipient> unverified)
  {
    return getPluralizedIdentityDescription(context, unverified,
                                            R.string.IdentityUtil_unverified_banner_one,
                                            R.string.IdentityUtil_unverified_banner_two,
                                            R.string.IdentityUtil_unverified_banner_many);
  }

  public static @Nullable String getUnverifiedSendDialogDescription(@NonNull Context context,
                                                                    @NonNull List<Recipient> unverified)
  {
    return getPluralizedIdentityDescription(context, unverified,
                                            R.string.IdentityUtil_unverified_dialog_one,
                                            R.string.IdentityUtil_unverified_dialog_two,
                                            R.string.IdentityUtil_unverified_dialog_many);
  }

  public static @Nullable String getUntrustedSendDialogDescription(@NonNull Context context,
                                                                   @NonNull List<Recipient> untrusted)
  {
    return getPluralizedIdentityDescription(context, untrusted,
                                            R.string.IdentityUtil_untrusted_dialog_one,
                                            R.string.IdentityUtil_untrusted_dialog_two,
                                            R.string.IdentityUtil_untrusted_dialog_many);
  }

  private static @Nullable String getPluralizedIdentityDescription(@NonNull Context context,
                                                                   @NonNull List<Recipient> recipients,
                                                                   @StringRes int resourceOne,
                                                                   @StringRes int resourceTwo,
                                                                   @StringRes int resourceMany)
  {
    if (recipients.isEmpty()) return null;

    if (recipients.size() == 1) {
      String name = recipients.get(0).getDisplayName(context);
      return context.getString(resourceOne, name);
    } else {
      String firstName  = recipients.get(0).getDisplayName(context);
      String secondName = recipients.get(1).getDisplayName(context);

      if (recipients.size() == 2) {
        return context.getString(resourceTwo, firstName, secondName);
      } else {
        int    othersCount = recipients.size() - 2;
        String nMore       = context.getResources().getQuantityString(R.plurals.identity_others, othersCount, othersCount);

        return context.getString(resourceMany, firstName, secondName, nMore);
      }
    }
  }
}
