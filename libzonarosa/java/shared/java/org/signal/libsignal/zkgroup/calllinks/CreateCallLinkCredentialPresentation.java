//
// Copyright 2023 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.zkgroup.calllinks;

import static io.zonarosa.libzonarosa.internal.FilterExceptions.filterExceptions;

import java.time.Instant;
import io.zonarosa.libzonarosa.internal.Native;
import io.zonarosa.libzonarosa.zkgroup.GenericServerSecretParams;
import io.zonarosa.libzonarosa.zkgroup.InvalidInputException;
import io.zonarosa.libzonarosa.zkgroup.VerificationFailedException;
import io.zonarosa.libzonarosa.zkgroup.internal.ByteArray;

public final class CreateCallLinkCredentialPresentation extends ByteArray {

  public CreateCallLinkCredentialPresentation(byte[] contents) throws InvalidInputException {
    super(contents);
    filterExceptions(
        InvalidInputException.class,
        () -> Native.CreateCallLinkCredentialPresentation_CheckValidContents(contents));
  }

  public void verify(
      byte[] roomId, GenericServerSecretParams serverParams, CallLinkPublicParams callLinkParams)
      throws VerificationFailedException {
    verify(roomId, Instant.now(), serverParams, callLinkParams);
  }

  public void verify(
      byte[] roomId,
      Instant currentTime,
      GenericServerSecretParams serverParams,
      CallLinkPublicParams callLinkParams)
      throws VerificationFailedException {
    filterExceptions(
        VerificationFailedException.class,
        () ->
            Native.CreateCallLinkCredentialPresentation_Verify(
                getInternalContentsForJNI(),
                roomId,
                currentTime.getEpochSecond(),
                serverParams.getInternalContentsForJNI(),
                callLinkParams.getInternalContentsForJNI()));
  }
}
