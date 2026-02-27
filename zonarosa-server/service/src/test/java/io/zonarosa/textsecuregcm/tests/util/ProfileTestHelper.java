package io.zonarosa.server.tests.util;

import java.util.Base64;
import io.zonarosa.server.util.TestRandomUtil;

public class ProfileTestHelper {
  public static String generateRandomBase64FromByteArray(final int byteArrayLength) {
    return encodeToBase64(TestRandomUtil.nextBytes(byteArrayLength));
  }

  public static String encodeToBase64(final byte[] input) {
    return Base64.getEncoder().encodeToString(input);
  }
}
