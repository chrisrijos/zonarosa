//
// Copyright 2023 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.zkgroup.calllinks;

import static io.zonarosa.libzonarosa.internal.FilterExceptions.filterExceptions;
import static io.zonarosa.libzonarosa.zkgroup.internal.Constants.RANDOM_LENGTH;

import java.security.SecureRandom;
import io.zonarosa.libzonarosa.internal.Native;
import io.zonarosa.libzonarosa.protocol.ServiceId.Aci;
import io.zonarosa.libzonarosa.zkgroup.GenericServerPublicParams;
import io.zonarosa.libzonarosa.zkgroup.InvalidInputException;
import io.zonarosa.libzonarosa.zkgroup.VerificationFailedException;
import io.zonarosa.libzonarosa.zkgroup.internal.ByteArray;

public final class CreateCallLinkCredentialRequestContext extends ByteArray {

  public CreateCallLinkCredentialRequestContext(byte[] contents) throws InvalidInputException {
    super(contents);
    filterExceptions(
        InvalidInputException.class,
        () -> Native.CreateCallLinkCredentialRequestContext_CheckValidContents(contents));
  }

  public static CreateCallLinkCredentialRequestContext forRoom(byte[] roomId) {
    return forRoom(roomId, new SecureRandom());
  }

  public static CreateCallLinkCredentialRequestContext forRoom(
      byte[] roomId, SecureRandom secureRandom) {
    byte[] random = new byte[RANDOM_LENGTH];
    secureRandom.nextBytes(random);

    byte[] newContents =
        Native.CreateCallLinkCredentialRequestContext_NewDeterministic(roomId, random);

    try {
      return new CreateCallLinkCredentialRequestContext(newContents);
    } catch (InvalidInputException e) {
      throw new AssertionError(e);
    }
  }

  public CreateCallLinkCredentialRequest getRequest() {
    byte[] newContents = Native.CreateCallLinkCredentialRequestContext_GetRequest(contents);

    try {
      return new CreateCallLinkCredentialRequest(newContents);
    } catch (InvalidInputException e) {
      throw new AssertionError(e);
    }
  }

  public CreateCallLinkCredential receiveResponse(
      CreateCallLinkCredentialResponse response, Aci userId, GenericServerPublicParams params)
      throws VerificationFailedException {
    byte[] newContents =
        filterExceptions(
            VerificationFailedException.class,
            () ->
                Native.CreateCallLinkCredentialRequestContext_ReceiveResponse(
                    getInternalContentsForJNI(),
                    response.getInternalContentsForJNI(),
                    userId.toServiceIdFixedWidthBinary(),
                    params.getInternalContentsForJNI()));

    try {
      return new CreateCallLinkCredential(newContents);
    } catch (InvalidInputException e) {
      throw new AssertionError(e);
    }
  }
}
