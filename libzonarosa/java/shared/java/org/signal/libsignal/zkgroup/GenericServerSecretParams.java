//
// Copyright 2023 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.zkgroup;

import static io.zonarosa.libzonarosa.internal.FilterExceptions.filterExceptions;
import static io.zonarosa.libzonarosa.zkgroup.internal.Constants.RANDOM_LENGTH;

import java.security.SecureRandom;
import io.zonarosa.libzonarosa.internal.Native;
import io.zonarosa.libzonarosa.zkgroup.internal.ByteArray;

public final class GenericServerSecretParams extends ByteArray {

  public static GenericServerSecretParams generate() {
    return generate(new SecureRandom());
  }

  public static GenericServerSecretParams generate(SecureRandom secureRandom) {
    byte[] random = new byte[RANDOM_LENGTH];
    secureRandom.nextBytes(random);

    byte[] newContents = Native.GenericServerSecretParams_GenerateDeterministic(random);

    try {
      return new GenericServerSecretParams(newContents);
    } catch (InvalidInputException e) {
      throw new AssertionError(e);
    }
  }

  public GenericServerSecretParams(byte[] contents) throws InvalidInputException {
    super(contents);
    filterExceptions(
        InvalidInputException.class,
        () -> Native.GenericServerSecretParams_CheckValidContents(contents));
  }

  public GenericServerPublicParams getPublicParams() {
    byte[] newContents = Native.GenericServerSecretParams_GetPublicParams(contents);
    try {
      return new GenericServerPublicParams(newContents);
    } catch (InvalidInputException e) {
      throw new AssertionError(e);
    }
  }
}
