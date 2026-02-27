package io.zonarosa.messenger.crypto;

import androidx.annotation.NonNull;

import io.zonarosa.libzonarosa.protocol.ZonaRosaProtocolAddress;
import io.zonarosa.messenger.database.ZonaRosaDatabase;
import io.zonarosa.messenger.dependencies.AppDependencies;
import io.zonarosa.messenger.keyvalue.ZonaRosaStore;
import io.zonarosa.service.api.ZonaRosaSessionLock;
import io.zonarosa.service.api.push.DistributionId;

public final class SenderKeyUtil {
  private SenderKeyUtil() {}

  /**
   * Clears the state for a sender key session we created. It will naturally get re-created when it is next needed, rotating the key.
   */
  public static void rotateOurKey(@NonNull DistributionId distributionId) {
    try (ZonaRosaSessionLock.Lock unused = ReentrantSessionLock.INSTANCE.acquire()) {
      AppDependencies.getProtocolStore().aci().senderKeys().deleteAllFor(ZonaRosaStore.account().requireAci().toString(), distributionId);
      ZonaRosaDatabase.senderKeyShared().deleteAllFor(distributionId);
    }
  }

  /**
   * Gets when the sender key session was created, or -1 if it doesn't exist.
   */
  public static long getCreateTimeForOurKey(@NonNull DistributionId distributionId) {
    ZonaRosaProtocolAddress address = new ZonaRosaProtocolAddress(ZonaRosaStore.account().requireAci().toString(), ZonaRosaStore.account().getDeviceId());
    return ZonaRosaDatabase.senderKeys().getCreatedTime(address, distributionId);
  }

  /**
   * Deletes all stored state around session keys. Should only really be used when the user is re-registering.
   */
  public static void clearAllState() {
    try (ZonaRosaSessionLock.Lock unused = ReentrantSessionLock.INSTANCE.acquire()) {
      AppDependencies.getProtocolStore().aci().senderKeys().deleteAll();
      ZonaRosaDatabase.senderKeyShared().deleteAll();
    }
  }
}
