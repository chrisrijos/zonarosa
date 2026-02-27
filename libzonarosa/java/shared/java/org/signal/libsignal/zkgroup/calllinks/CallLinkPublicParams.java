//
// Copyright 2023 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.zkgroup.calllinks;

import static io.zonarosa.libzonarosa.internal.FilterExceptions.filterExceptions;

import io.zonarosa.libzonarosa.internal.Native;
import io.zonarosa.libzonarosa.zkgroup.InvalidInputException;
import io.zonarosa.libzonarosa.zkgroup.internal.ByteArray;

public final class CallLinkPublicParams extends ByteArray {
  public CallLinkPublicParams(byte[] contents) throws InvalidInputException {
    super(contents);
    filterExceptions(
        InvalidInputException.class,
        () -> Native.CallLinkPublicParams_CheckValidContents(contents));
  }
}
