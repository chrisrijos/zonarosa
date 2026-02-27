/**
 * Copyright (C) 2014-2016 ZonaRosa Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */

package io.zonarosa.service.api.util;

import io.zonarosa.core.models.ServiceId.ACI;
import io.zonarosa.core.models.ServiceId.PNI;
import io.zonarosa.service.api.push.ZonaRosaServiceAddress;

public interface CredentialsProvider {
  ACI getAci();
  PNI getPni();
  String getE164();
  int getDeviceId();
  String getPassword();

  default boolean isInvalid() {
    return getAci() == null || getPassword() == null;
  }

  default String getUsername() {
    StringBuilder sb = new StringBuilder();
    sb.append(getAci().toString());
    if (getDeviceId() != ZonaRosaServiceAddress.DEFAULT_DEVICE_ID) {
      sb.append(".");
      sb.append(getDeviceId());
    }
    return sb.toString();
  }
}
