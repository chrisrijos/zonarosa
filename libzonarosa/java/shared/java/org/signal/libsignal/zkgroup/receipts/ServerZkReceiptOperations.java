//
// Copyright 2020-2021 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.zkgroup.receipts;

import static io.zonarosa.libzonarosa.internal.FilterExceptions.filterExceptions;
import static io.zonarosa.libzonarosa.zkgroup.internal.Constants.RANDOM_LENGTH;

import java.security.SecureRandom;
import io.zonarosa.libzonarosa.internal.Native;
import io.zonarosa.libzonarosa.zkgroup.InvalidInputException;
import io.zonarosa.libzonarosa.zkgroup.ServerSecretParams;
import io.zonarosa.libzonarosa.zkgroup.VerificationFailedException;

public class ServerZkReceiptOperations {

  private final ServerSecretParams serverSecretParams;

  public ServerZkReceiptOperations(ServerSecretParams serverSecretParams) {
    this.serverSecretParams = serverSecretParams;
  }

  public ReceiptCredentialResponse issueReceiptCredential(
      ReceiptCredentialRequest receiptCredentialRequest,
      long receiptExpirationTime,
      long receiptLevel)
      throws VerificationFailedException {
    return issueReceiptCredential(
        new SecureRandom(), receiptCredentialRequest, receiptExpirationTime, receiptLevel);
  }

  public ReceiptCredentialResponse issueReceiptCredential(
      SecureRandom secureRandom,
      ReceiptCredentialRequest receiptCredentialRequest,
      long receiptExpirationTime,
      long receiptLevel)
      throws VerificationFailedException {
    byte[] random = new byte[RANDOM_LENGTH];
    secureRandom.nextBytes(random);

    byte[] newContents =
        serverSecretParams.guardedMap(
            (serverSecretParams) ->
                Native.ServerSecretParams_IssueReceiptCredentialDeterministic(
                    serverSecretParams,
                    random,
                    receiptCredentialRequest.getInternalContentsForJNI(),
                    receiptExpirationTime,
                    receiptLevel));

    try {
      return new ReceiptCredentialResponse(newContents);
    } catch (InvalidInputException e) {
      throw new AssertionError(e);
    }
  }

  public void verifyReceiptCredentialPresentation(
      ReceiptCredentialPresentation receiptCredentialPresentation)
      throws VerificationFailedException {
    filterExceptions(
        VerificationFailedException.class,
        () ->
            serverSecretParams.guardedRunChecked(
                (secretParams) ->
                    Native.ServerSecretParams_VerifyReceiptCredentialPresentation(
                        secretParams, receiptCredentialPresentation.getInternalContentsForJNI())));
  }
}
