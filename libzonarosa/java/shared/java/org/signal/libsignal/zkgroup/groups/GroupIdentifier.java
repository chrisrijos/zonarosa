//
// Copyright 2020-2021 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.zkgroup.groups;

import io.zonarosa.libzonarosa.zkgroup.InvalidInputException;
import io.zonarosa.libzonarosa.zkgroup.internal.ByteArray;

public final class GroupIdentifier extends ByteArray {

  public static final int SIZE = 32;

  public GroupIdentifier(byte[] contents) throws InvalidInputException {
    super(contents, SIZE);
  }
}
