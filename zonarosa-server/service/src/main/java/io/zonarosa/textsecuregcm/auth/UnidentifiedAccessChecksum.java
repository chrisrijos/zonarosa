/*
 * Copyright 2013-2020 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.auth;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class UnidentifiedAccessChecksum {

  public static byte[] generateFor(byte[] unidentifiedAccessKey) {
    try {
      if (unidentifiedAccessKey.length != UnidentifiedAccessUtil.UNIDENTIFIED_ACCESS_KEY_LENGTH) {
        throw new IllegalArgumentException("Invalid UAK length: " + unidentifiedAccessKey.length);
      }

      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(unidentifiedAccessKey, "HmacSHA256"));

      return mac.doFinal(new byte[32]);
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new AssertionError(e);
    }
  }
}
