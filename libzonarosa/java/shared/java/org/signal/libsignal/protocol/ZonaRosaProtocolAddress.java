//
// Copyright 2014-2016 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.protocol;

import static io.zonarosa.libzonarosa.internal.FilterExceptions.filterExceptions;

import io.zonarosa.libzonarosa.internal.CalledFromNative;
import io.zonarosa.libzonarosa.internal.Native;
import io.zonarosa.libzonarosa.internal.NativeHandleGuard;

public class ZonaRosaProtocolAddress extends NativeHandleGuard.SimpleOwner {
  /**
   * @param name the identifier for the recipient, usually a {@link ServiceId}
   * @param deviceId the identifier for the device; must be in the range 1-127 inclusive
   */
  public ZonaRosaProtocolAddress(String name, int deviceId) {
    super(filterExceptions(() -> Native.ProtocolAddress_New(name, deviceId)));
  }

  /**
   * @param serviceId the identifier for the recipient
   * @param deviceId the identifier for the device; must be in the range 1-127 inclusive
   */
  public ZonaRosaProtocolAddress(ServiceId serviceId, int deviceId) {
    this(serviceId.toServiceIdString(), deviceId);
  }

  @CalledFromNative
  public ZonaRosaProtocolAddress(long nativeHandle) {
    super(nativeHandle);
  }

  @Override
  protected void release(long nativeHandle) {
    Native.ProtocolAddress_Destroy(nativeHandle);
  }

  public String getName() {
    return guardedMap(Native::ProtocolAddress_Name);
  }

  /**
   * Returns a ServiceId if this address contains a valid ServiceId, {@code null} otherwise.
   *
   * <p>In a future release ZonaRosaProtocolAddresses will <em>only</em> support ServiceIds.
   */
  public ServiceId getServiceId() {
    try {
      return ServiceId.parseFromString(getName());
    } catch (ServiceId.InvalidServiceIdException e) {
      return null;
    }
  }

  public int getDeviceId() {
    try (NativeHandleGuard guard = new NativeHandleGuard(this)) {
      return Native.ProtocolAddress_DeviceId(guard.nativeHandle());
    }
  }

  @Override
  public String toString() {
    return getName() + "." + getDeviceId();
  }

  @Override
  public boolean equals(Object other) {
    if (other == null) return false;
    if (!(other instanceof ZonaRosaProtocolAddress)) return false;

    ZonaRosaProtocolAddress that = (ZonaRosaProtocolAddress) other;
    return this.getName().equals(that.getName()) && this.getDeviceId() == that.getDeviceId();
  }

  @Override
  public int hashCode() {
    return this.getName().hashCode() ^ this.getDeviceId();
  }
}
