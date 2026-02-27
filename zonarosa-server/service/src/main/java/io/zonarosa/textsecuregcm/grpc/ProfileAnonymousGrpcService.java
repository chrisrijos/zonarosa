/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.grpc;

import io.grpc.Status;
import io.grpc.StatusException;
import java.time.Clock;
import io.zonarosa.chat.profile.CredentialType;
import io.zonarosa.chat.profile.GetExpiringProfileKeyCredentialAnonymousRequest;
import io.zonarosa.chat.profile.GetExpiringProfileKeyCredentialResponse;
import io.zonarosa.chat.profile.GetUnversionedProfileAnonymousRequest;
import io.zonarosa.chat.profile.GetUnversionedProfileResponse;
import io.zonarosa.chat.profile.GetVersionedProfileAnonymousRequest;
import io.zonarosa.chat.profile.GetVersionedProfileResponse;
import io.zonarosa.chat.profile.SimpleProfileAnonymousGrpc;
import io.zonarosa.libzonarosa.zkgroup.ServerSecretParams;
import io.zonarosa.libzonarosa.zkgroup.profiles.ServerZkProfileOperations;
import io.zonarosa.server.auth.UnidentifiedAccessUtil;
import io.zonarosa.server.badges.ProfileBadgeConverter;
import io.zonarosa.server.identity.IdentityType;
import io.zonarosa.server.identity.ServiceIdentifier;
import io.zonarosa.server.storage.Account;
import io.zonarosa.server.storage.AccountsManager;
import io.zonarosa.server.storage.ProfilesManager;

public class ProfileAnonymousGrpcService extends SimpleProfileAnonymousGrpc.ProfileAnonymousImplBase {
  private final AccountsManager accountsManager;
  private final ProfilesManager profilesManager;
  private final ProfileBadgeConverter profileBadgeConverter;
  private final ServerZkProfileOperations zkProfileOperations;
  private final GroupSendTokenUtil groupSendTokenUtil;

  public ProfileAnonymousGrpcService(
      final AccountsManager accountsManager,
      final ProfilesManager profilesManager,
      final ProfileBadgeConverter profileBadgeConverter,
      final ServerSecretParams serverSecretParams) {
    this.accountsManager = accountsManager;
    this.profilesManager = profilesManager;
    this.profileBadgeConverter = profileBadgeConverter;
    this.zkProfileOperations = new ServerZkProfileOperations(serverSecretParams);
    this.groupSendTokenUtil = new GroupSendTokenUtil(serverSecretParams, Clock.systemUTC());
  }

  @Override
  public GetUnversionedProfileResponse getUnversionedProfile(final GetUnversionedProfileAnonymousRequest request) throws StatusException {
    final ServiceIdentifier targetIdentifier =
        ServiceIdentifierUtil.fromGrpcServiceIdentifier(request.getRequest().getServiceIdentifier());

    // Callers must be authenticated to request unversioned profiles by PNI
    if (targetIdentifier.identityType() == IdentityType.PNI) {
      throw Status.UNAUTHENTICATED.asException();
    }

    final Account account = switch (request.getAuthenticationCase()) {
      case GROUP_SEND_TOKEN -> {
        if (!groupSendTokenUtil.checkGroupSendToken(request.getGroupSendToken(), targetIdentifier)) {
          throw Status.UNAUTHENTICATED.asException();
        }
        yield accountsManager.getByServiceIdentifier(targetIdentifier)
            .orElseThrow(Status.NOT_FOUND::asException);
      }
      case UNIDENTIFIED_ACCESS_KEY ->
          getTargetAccountAndValidateUnidentifiedAccess(targetIdentifier, request.getUnidentifiedAccessKey().toByteArray());
      default -> throw Status.INVALID_ARGUMENT.asException();
    };

    return ProfileGrpcHelper.buildUnversionedProfileResponse(targetIdentifier,
            null,
            account,
            profileBadgeConverter);
  }

  @Override
  public GetVersionedProfileResponse getVersionedProfile(final GetVersionedProfileAnonymousRequest request) throws StatusException {
    final ServiceIdentifier targetIdentifier = ServiceIdentifierUtil.fromGrpcServiceIdentifier(request.getRequest().getAccountIdentifier());

    if (targetIdentifier.identityType() != IdentityType.ACI) {
      throw Status.INVALID_ARGUMENT.withDescription("Expected ACI service identifier").asException();
    }

    final Account targetAccount = getTargetAccountAndValidateUnidentifiedAccess(targetIdentifier, request.getUnidentifiedAccessKey().toByteArray());
    return ProfileGrpcHelper.getVersionedProfile(targetAccount, profilesManager, request.getRequest().getVersion());
  }

  @Override
  public GetExpiringProfileKeyCredentialResponse getExpiringProfileKeyCredential(
      final GetExpiringProfileKeyCredentialAnonymousRequest request) throws StatusException {
    final ServiceIdentifier targetIdentifier = ServiceIdentifierUtil.fromGrpcServiceIdentifier(request.getRequest().getAccountIdentifier());

    if (targetIdentifier.identityType() != IdentityType.ACI) {
      throw Status.INVALID_ARGUMENT.withDescription("Expected ACI service identifier").asException();
    }

    if (request.getRequest().getCredentialType() != CredentialType.CREDENTIAL_TYPE_EXPIRING_PROFILE_KEY) {
      throw Status.INVALID_ARGUMENT.withDescription("Expected expiring profile key credential type").asException();
    }

    final Account account = getTargetAccountAndValidateUnidentifiedAccess(
        targetIdentifier, request.getUnidentifiedAccessKey().toByteArray());
    return ProfileGrpcHelper.getExpiringProfileKeyCredentialResponse(account.getUuid(),
            request.getRequest().getVersion(), request.getRequest().getCredentialRequest().toByteArray(), profilesManager, zkProfileOperations);
  }

  private Account getTargetAccountAndValidateUnidentifiedAccess(final ServiceIdentifier targetIdentifier, final byte[] unidentifiedAccessKey) throws StatusException {

    return accountsManager.getByServiceIdentifier(targetIdentifier)
        .filter(targetAccount -> UnidentifiedAccessUtil.checkUnidentifiedAccess(targetAccount, unidentifiedAccessKey))
        .orElseThrow(Status.UNAUTHENTICATED::asException);
  }
}
