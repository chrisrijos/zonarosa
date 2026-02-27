/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.grpc;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.ByteString;
import io.grpc.Status;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import io.grpc.StatusException;
import io.zonarosa.chat.profile.Badge;
import io.zonarosa.chat.profile.BadgeSvg;
import io.zonarosa.chat.profile.GetExpiringProfileKeyCredentialResponse;
import io.zonarosa.chat.profile.GetUnversionedProfileResponse;
import io.zonarosa.chat.profile.GetVersionedProfileResponse;
import io.zonarosa.libzonarosa.protocol.ServiceId;
import io.zonarosa.libzonarosa.zkgroup.InvalidInputException;
import io.zonarosa.libzonarosa.zkgroup.VerificationFailedException;
import io.zonarosa.libzonarosa.zkgroup.profiles.ExpiringProfileKeyCredentialResponse;
import io.zonarosa.libzonarosa.zkgroup.profiles.ServerZkProfileOperations;
import io.zonarosa.server.auth.UnidentifiedAccessChecksum;
import io.zonarosa.server.badges.ProfileBadgeConverter;
import io.zonarosa.server.identity.ServiceIdentifier;
import io.zonarosa.server.storage.Account;
import io.zonarosa.server.storage.DeviceCapability;
import io.zonarosa.server.storage.ProfilesManager;
import io.zonarosa.server.storage.VersionedProfile;
import io.zonarosa.server.util.ProfileHelper;

public class ProfileGrpcHelper {
  static GetVersionedProfileResponse getVersionedProfile(final Account account,
      final ProfilesManager profilesManager,
      final String requestVersion) throws StatusException {

    final VersionedProfile profile = profilesManager.get(account.getUuid(), requestVersion)
        .orElseThrow(Status.NOT_FOUND.withDescription("Profile version not found")::asException);

    final GetVersionedProfileResponse.Builder responseBuilder = GetVersionedProfileResponse.newBuilder();

    responseBuilder
        .setName(ByteString.copyFrom(profile.name()))
        .setAbout(ByteString.copyFrom(profile.about()))
        .setAboutEmoji(ByteString.copyFrom(profile.aboutEmoji()))
        .setAvatar(profile.avatar())
        .setPhoneNumberSharing(ByteString.copyFrom(profile.phoneNumberSharing()));

    // Allow requests where either the version matches the latest version on Account or the latest version on Account
    // is empty to read the payment address.
    if (account.getCurrentProfileVersion().map(v -> v.equals(requestVersion)).orElse(true)) {
      responseBuilder.setPaymentAddress(ByteString.copyFrom(profile.paymentAddress()));
    }

    return responseBuilder.build();
  }

  @VisibleForTesting
  static List<Badge> buildBadges(final List<io.zonarosa.server.entities.Badge> badges) {
    final ArrayList<Badge> grpcBadges = new ArrayList<>();
    for (final io.zonarosa.server.entities.Badge badge : badges) {
      grpcBadges.add(Badge.newBuilder()
          .setId(badge.getId())
          .setCategory(badge.getCategory())
          .setName(badge.getName())
          .setDescription(badge.getDescription())
          .addAllSprites6(badge.getSprites6())
          .setSvg(badge.getSvg())
          .addAllSvgs(buildBadgeSvgs(badge.getSvgs()))
          .build());
    }
    return grpcBadges;
  }

  @VisibleForTesting
  static List<io.zonarosa.chat.common.DeviceCapability> buildAccountCapabilities(final Account account) {
    return Arrays.stream(DeviceCapability.values())
        .filter(DeviceCapability::includeInProfile)
        .filter(account::hasCapability)
        .map(DeviceCapabilityUtil::toGrpcDeviceCapability)
        .toList();
  }

  private static List<BadgeSvg> buildBadgeSvgs(final List<io.zonarosa.server.entities.BadgeSvg> badgeSvgs) {
    ArrayList<BadgeSvg> grpcBadgeSvgs = new ArrayList<>();
    for (final io.zonarosa.server.entities.BadgeSvg badgeSvg : badgeSvgs) {
      grpcBadgeSvgs.add(BadgeSvg.newBuilder()
          .setDark(badgeSvg.getDark())
          .setLight(badgeSvg.getLight())
          .build());
    }
    return grpcBadgeSvgs;
  }

  static GetUnversionedProfileResponse buildUnversionedProfileResponse(
      final ServiceIdentifier targetIdentifier,
      final UUID requesterUuid,
      final Account targetAccount,
      final ProfileBadgeConverter profileBadgeConverter) {
    final GetUnversionedProfileResponse.Builder responseBuilder = GetUnversionedProfileResponse.newBuilder()
        .setIdentityKey(ByteString.copyFrom(targetAccount.getIdentityKey(targetIdentifier.identityType()).serialize()))
        .addAllCapabilities(buildAccountCapabilities(targetAccount));

    switch (targetIdentifier.identityType()) {
      case ACI -> {
        responseBuilder.setUnrestrictedUnidentifiedAccess(targetAccount.isUnrestrictedUnidentifiedAccess())
            .addAllBadges(buildBadges(profileBadgeConverter.convert(
                RequestAttributesUtil.getAvailableAcceptedLocales(),
                targetAccount.getBadges(),
                ProfileHelper.isSelfProfileRequest(requesterUuid, targetIdentifier))));

        targetAccount.getUnidentifiedAccessKey()
            .map(UnidentifiedAccessChecksum::generateFor)
            .map(ByteString::copyFrom)
            .ifPresent(responseBuilder::setUnidentifiedAccess);
      }
      case PNI -> responseBuilder.setUnrestrictedUnidentifiedAccess(false);
    }

    return responseBuilder.build();
  }

  static GetExpiringProfileKeyCredentialResponse getExpiringProfileKeyCredentialResponse(
      final UUID targetUuid,
      final String version,
      final byte[] encodedCredentialRequest,
      final ProfilesManager profilesManager,
      final ServerZkProfileOperations zkProfileOperations) throws StatusException {

    final VersionedProfile profile = profilesManager.get(targetUuid, version)
        .orElseThrow(Status.NOT_FOUND.withDescription("Profile version not found")::asException);

    final ExpiringProfileKeyCredentialResponse profileKeyCredentialResponse;
    try {
      profileKeyCredentialResponse = ProfileHelper.getExpiringProfileKeyCredential(encodedCredentialRequest,
          profile, new ServiceId.Aci(targetUuid), zkProfileOperations);
    } catch (VerificationFailedException | InvalidInputException e) {
      throw Status.INVALID_ARGUMENT.withCause(e).asException();
    }

    return GetExpiringProfileKeyCredentialResponse.newBuilder()
        .setProfileKeyCredential(ByteString.copyFrom(profileKeyCredentialResponse.serialize()))
        .build();
  }
}
