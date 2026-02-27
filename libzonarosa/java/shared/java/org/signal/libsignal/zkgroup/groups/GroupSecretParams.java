//
// Copyright 2020-2021 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.zkgroup.groups;

import static io.zonarosa.libzonarosa.internal.FilterExceptions.filterExceptions;
import static io.zonarosa.libzonarosa.zkgroup.internal.Constants.RANDOM_LENGTH;

import java.security.SecureRandom;
import io.zonarosa.libzonarosa.internal.Native;
import io.zonarosa.libzonarosa.zkgroup.InvalidInputException;
import io.zonarosa.libzonarosa.zkgroup.internal.ByteArray;

public final class GroupSecretParams extends ByteArray {

  public static GroupSecretParams generate() {
    return generate(new SecureRandom());
  }

  public static GroupSecretParams generate(SecureRandom secureRandom) {
    byte[] random = new byte[RANDOM_LENGTH];
    secureRandom.nextBytes(random);

    byte[] newContents = Native.GroupSecretParams_GenerateDeterministic(random);

    try {
      return new GroupSecretParams(newContents);
    } catch (InvalidInputException e) {
      throw new AssertionError(e);
    }
  }

  public static GroupSecretParams deriveFromMasterKey(GroupMasterKey groupMasterKey) {
    byte[] newContents =
        Native.GroupSecretParams_DeriveFromMasterKey(groupMasterKey.getInternalContentsForJNI());

    try {
      return new GroupSecretParams(newContents);
    } catch (InvalidInputException e) {
      throw new AssertionError(e);
    }
  }

  public GroupSecretParams(byte[] contents) throws InvalidInputException {
    super(contents);
    filterExceptions(
        InvalidInputException.class, () -> Native.GroupSecretParams_CheckValidContents(contents));
  }

  public GroupMasterKey getMasterKey() {
    byte[] newContents = Native.GroupSecretParams_GetMasterKey(contents);

    try {
      return new GroupMasterKey(newContents);
    } catch (InvalidInputException e) {
      throw new AssertionError(e);
    }
  }

  public GroupPublicParams getPublicParams() {
    byte[] newContents = Native.GroupSecretParams_GetPublicParams(contents);

    try {
      return new GroupPublicParams(newContents);
    } catch (InvalidInputException e) {
      throw new AssertionError(e);
    }
  }
}
