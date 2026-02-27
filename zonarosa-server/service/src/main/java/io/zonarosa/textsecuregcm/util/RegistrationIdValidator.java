/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 *
 */

package io.zonarosa.server.util;

import io.zonarosa.server.storage.Device;

public class RegistrationIdValidator {
  public static boolean validRegistrationId(int registrationId) {
    return registrationId > 0 && registrationId <= Device.MAX_REGISTRATION_ID;
  }
}
