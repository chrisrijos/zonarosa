package io.zonarosa.messenger.crypto.storage;

import android.content.Context;

import androidx.annotation.NonNull;

import io.zonarosa.libzonarosa.protocol.ZonaRosaProtocolAddress;
import io.zonarosa.libzonarosa.protocol.groups.state.SenderKeyRecord;
import io.zonarosa.messenger.crypto.ReentrantSessionLock;
import io.zonarosa.messenger.database.SenderKeyTable;
import io.zonarosa.messenger.database.ZonaRosaDatabase;
import io.zonarosa.service.api.ZonaRosaServiceSenderKeyStore;
import io.zonarosa.service.api.ZonaRosaSessionLock;
import io.zonarosa.service.api.push.DistributionId;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

/**
 * An implementation of the storage interface used by the protocol layer to store sender keys. For
 * more details around sender keys, see {@link SenderKeyTable}.
 */
public final class ZonaRosaSenderKeyStore implements ZonaRosaServiceSenderKeyStore {

  private final Context context;

  public ZonaRosaSenderKeyStore(@NonNull Context context) {
    this.context = context;
  }

  @Override
  public void storeSenderKey(@NonNull ZonaRosaProtocolAddress sender, @NonNull UUID distributionId, @NonNull SenderKeyRecord record) {
    try (ZonaRosaSessionLock.Lock unused = ReentrantSessionLock.INSTANCE.acquire()) {
      ZonaRosaDatabase.senderKeys().store(sender, DistributionId.from(distributionId), record);
    }
  }

  @Override
  public @Nullable SenderKeyRecord loadSenderKey(@NonNull ZonaRosaProtocolAddress sender, @NonNull UUID distributionId) {
    try (ZonaRosaSessionLock.Lock unused = ReentrantSessionLock.INSTANCE.acquire()) {
      return ZonaRosaDatabase.senderKeys().load(sender, DistributionId.from(distributionId));
    }
  }

  @Override
  public Set<ZonaRosaProtocolAddress> getSenderKeySharedWith(DistributionId distributionId) {
    try (ZonaRosaSessionLock.Lock unused = ReentrantSessionLock.INSTANCE.acquire()) {
      return ZonaRosaDatabase.senderKeyShared().getSharedWith(distributionId);
    }
  }

  @Override
  public void markSenderKeySharedWith(DistributionId distributionId, Collection<ZonaRosaProtocolAddress> addresses) {
    try (ZonaRosaSessionLock.Lock unused = ReentrantSessionLock.INSTANCE.acquire()) {
      ZonaRosaDatabase.senderKeyShared().markAsShared(distributionId, addresses);
    }
  }

  @Override
  public void clearSenderKeySharedWith(Collection<ZonaRosaProtocolAddress> addresses) {
    try (ZonaRosaSessionLock.Lock unused = ReentrantSessionLock.INSTANCE.acquire()) {
      ZonaRosaDatabase.senderKeyShared().deleteAllFor(addresses);
    }
  }

  /**
   * Removes all sender key session state for all devices for the provided recipient-distributionId pair.
   */
  public void deleteAllFor(@NonNull String addressName, @NonNull DistributionId distributionId) {
    try (ZonaRosaSessionLock.Lock unused = ReentrantSessionLock.INSTANCE.acquire()) {
      ZonaRosaDatabase.senderKeys().deleteAllFor(addressName, distributionId);
    }
  }

  /**
   * Deletes all sender key session state.
   */
  public void deleteAll() {
    try (ZonaRosaSessionLock.Lock unused = ReentrantSessionLock.INSTANCE.acquire()) {
      ZonaRosaDatabase.senderKeys().deleteAll();
    }
  }
}