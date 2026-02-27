//
// Copyright 2020-2021 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.zkgroup;

import static org.junit.Assert.assertArrayEquals;

import java.io.IOException;
import java.security.SecureRandom;
import org.junit.Test;
import io.zonarosa.libzonarosa.protocol.util.Hex;
import io.zonarosa.libzonarosa.zkgroup.internal.*;

public final class RandomnessTest extends SecureRandomTest {

  @Test
  public void generate_usesSecureRandom() throws IOException {
    byte[] array =
        Hex.fromStringCondensed("e18de7dfe7195f0b9320e309cd3ed3765dcf54a09be57813ee69f5ea35867689");
    SecureRandom secureRandom = createSecureRandom(array);
    byte[] random = new byte[array.length];
    secureRandom.nextBytes(random);

    assertArrayEquals(array, random);
  }

  @Test
  public void generate_usesSecureRandom_alternativeValues() throws IOException {
    byte[] array =
        Hex.fromStringCondensed("ba8a89a05eaf51cac3ce35256199b38a18e0e1fa16f1443db8e34b0489739b80");
    SecureRandom secureRandom = createSecureRandom(array);

    byte[] random = new byte[array.length];
    secureRandom.nextBytes(random);

    assertArrayEquals(array, random);
  }
}
