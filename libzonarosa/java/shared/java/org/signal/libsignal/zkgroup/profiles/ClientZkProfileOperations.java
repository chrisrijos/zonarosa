//
// Copyright 2020-2022 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.zkgroup.profiles;

import static io.zonarosa.libzonarosa.internal.FilterExceptions.filterExceptions;
import static io.zonarosa.libzonarosa.zkgroup.internal.Constants.RANDOM_LENGTH;

import java.security.SecureRandom;
import java.time.Instant;
import io.zonarosa.libzonarosa.internal.Native;
import io.zonarosa.libzonarosa.protocol.ServiceId.Aci;
import io.zonarosa.libzonarosa.zkgroup.InvalidInputException;
import io.zonarosa.libzonarosa.zkgroup.ServerPublicParams;
import io.zonarosa.libzonarosa.zkgroup.VerificationFailedException;
import io.zonarosa.libzonarosa.zkgroup.groups.GroupSecretParams;

public class ClientZkProfileOperations {

  private final ServerPublicParams serverPublicParams;

  public ClientZkProfileOperations(ServerPublicParams serverPublicParams) {
    this.serverPublicParams = serverPublicParams;
  }

  public ProfileKeyCredentialRequestContext createProfileKeyCredentialRequestContext(
      Aci userId, ProfileKey profileKey) {
    return createProfileKeyCredentialRequestContext(new SecureRandom(), userId, profileKey);
  }

  public ProfileKeyCredentialRequestContext createProfileKeyCredentialRequestContext(
      SecureRandom secureRandom, Aci userId, ProfileKey profileKey) {
    byte[] random = new byte[RANDOM_LENGTH];
    secureRandom.nextBytes(random);

    byte[] newContents =
        serverPublicParams.guardedMap(
            (serverPublicParams) ->
                Native.ServerPublicParams_CreateProfileKeyCredentialRequestContextDeterministic(
                    serverPublicParams,
                    random,
                    userId.toServiceIdFixedWidthBinary(),
                    profileKey.getInternalContentsForJNI()));

    try {
      return new ProfileKeyCredentialRequestContext(newContents);
    } catch (InvalidInputException e) {
      throw new AssertionError(e);
    }
  }

  public ExpiringProfileKeyCredential receiveExpiringProfileKeyCredential(
      ProfileKeyCredentialRequestContext profileKeyCredentialRequestContext,
      ExpiringProfileKeyCredentialResponse profileKeyCredentialResponse)
      throws VerificationFailedException {
    return receiveExpiringProfileKeyCredential(
        profileKeyCredentialRequestContext, profileKeyCredentialResponse, Instant.now());
  }

  public ExpiringProfileKeyCredential receiveExpiringProfileKeyCredential(
      ProfileKeyCredentialRequestContext profileKeyCredentialRequestContext,
      ExpiringProfileKeyCredentialResponse profileKeyCredentialResponse,
      Instant now)
      throws VerificationFailedException {
    if (profileKeyCredentialResponse == null) {
      throw new VerificationFailedException();
    }

    byte[] newContents =
        filterExceptions(
            VerificationFailedException.class,
            () ->
                serverPublicParams.guardedMapChecked(
                    (publicParams) ->
                        Native.ServerPublicParams_ReceiveExpiringProfileKeyCredential(
                            publicParams,
                            profileKeyCredentialRequestContext.getInternalContentsForJNI(),
                            profileKeyCredentialResponse.getInternalContentsForJNI(),
                            now.getEpochSecond())));

    try {
      return new ExpiringProfileKeyCredential(newContents);
    } catch (InvalidInputException e) {
      throw new AssertionError(e);
    }
  }

  public ProfileKeyCredentialPresentation createProfileKeyCredentialPresentation(
      GroupSecretParams groupSecretParams, ExpiringProfileKeyCredential profileKeyCredential) {
    return createProfileKeyCredentialPresentation(
        new SecureRandom(), groupSecretParams, profileKeyCredential);
  }

  public ProfileKeyCredentialPresentation createProfileKeyCredentialPresentation(
      SecureRandom secureRandom,
      GroupSecretParams groupSecretParams,
      ExpiringProfileKeyCredential profileKeyCredential) {
    byte[] random = new byte[RANDOM_LENGTH];
    secureRandom.nextBytes(random);

    byte[] newContents =
        serverPublicParams.guardedMap(
            (publicParams) ->
                Native
                    .ServerPublicParams_CreateExpiringProfileKeyCredentialPresentationDeterministic(
                        publicParams,
                        random,
                        groupSecretParams.getInternalContentsForJNI(),
                        profileKeyCredential.getInternalContentsForJNI()));

    try {
      return new ProfileKeyCredentialPresentation(newContents);
    } catch (InvalidInputException e) {
      throw new AssertionError(e);
    }
  }
}
