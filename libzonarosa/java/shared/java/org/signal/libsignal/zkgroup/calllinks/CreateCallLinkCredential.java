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
import io.zonarosa.libzonarosa.zkgroup.internal.ByteArray;

public final class CreateCallLinkCredential extends ByteArray {

  public CreateCallLinkCredential(byte[] contents) throws InvalidInputException {
    super(contents);
    filterExceptions(
        InvalidInputException.class,
        () -> Native.CreateCallLinkCredential_CheckValidContents(contents));
  }

  public CreateCallLinkCredentialPresentation present(
      byte[] roomId,
      Aci userId,
      GenericServerPublicParams serverParams,
      CallLinkSecretParams callLinkParams) {
    return present(roomId, userId, serverParams, callLinkParams, new SecureRandom());
  }

  public CreateCallLinkCredentialPresentation present(
      byte[] roomId,
      Aci userId,
      GenericServerPublicParams serverParams,
      CallLinkSecretParams callLinkParams,
      SecureRandom secureRandom) {
    byte[] random = new byte[RANDOM_LENGTH];
    secureRandom.nextBytes(random);

    byte[] newContents =
        filterExceptions(
            () ->
                Native.CreateCallLinkCredential_PresentDeterministic(
                    getInternalContentsForJNI(),
                    roomId,
                    userId.toServiceIdFixedWidthBinary(),
                    serverParams.getInternalContentsForJNI(),
                    callLinkParams.getInternalContentsForJNI(),
                    random));

    try {
      return new CreateCallLinkCredentialPresentation(newContents);
    } catch (InvalidInputException e) {
      throw new AssertionError(e);
    }
  }
}
