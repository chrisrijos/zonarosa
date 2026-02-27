/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.grpc;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatNoException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static io.zonarosa.server.grpc.GrpcTestUtils.assertStatusException;

import com.google.common.net.InetAddresses;
import com.google.protobuf.ByteString;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import io.zonarosa.chat.common.IdentityType;
import io.zonarosa.chat.common.ServiceIdentifier;
import io.zonarosa.chat.profile.CredentialType;
import io.zonarosa.chat.profile.GetExpiringProfileKeyCredentialAnonymousRequest;
import io.zonarosa.chat.profile.GetExpiringProfileKeyCredentialRequest;
import io.zonarosa.chat.profile.GetExpiringProfileKeyCredentialResponse;
import io.zonarosa.chat.profile.GetUnversionedProfileAnonymousRequest;
import io.zonarosa.chat.profile.GetUnversionedProfileRequest;
import io.zonarosa.chat.profile.GetUnversionedProfileResponse;
import io.zonarosa.chat.profile.GetVersionedProfileAnonymousRequest;
import io.zonarosa.chat.profile.GetVersionedProfileRequest;
import io.zonarosa.chat.profile.GetVersionedProfileResponse;
import io.zonarosa.chat.profile.ProfileAnonymousGrpc;
import io.zonarosa.libzonarosa.protocol.IdentityKey;
import io.zonarosa.libzonarosa.protocol.ServiceId;
import io.zonarosa.libzonarosa.protocol.ecc.ECKeyPair;
import io.zonarosa.libzonarosa.zkgroup.InvalidInputException;
import io.zonarosa.libzonarosa.zkgroup.ServerSecretParams;
import io.zonarosa.libzonarosa.zkgroup.VerificationFailedException;
import io.zonarosa.libzonarosa.zkgroup.profiles.ClientZkProfileOperations;
import io.zonarosa.libzonarosa.zkgroup.profiles.ExpiringProfileKeyCredentialResponse;
import io.zonarosa.libzonarosa.zkgroup.profiles.ProfileKey;
import io.zonarosa.libzonarosa.zkgroup.profiles.ProfileKeyCommitment;
import io.zonarosa.libzonarosa.zkgroup.profiles.ProfileKeyCredentialRequest;
import io.zonarosa.libzonarosa.zkgroup.profiles.ProfileKeyCredentialRequestContext;
import io.zonarosa.server.auth.UnidentifiedAccessChecksum;
import io.zonarosa.server.auth.UnidentifiedAccessUtil;
import io.zonarosa.server.badges.ProfileBadgeConverter;
import io.zonarosa.server.entities.Badge;
import io.zonarosa.server.entities.BadgeSvg;
import io.zonarosa.server.identity.AciServiceIdentifier;
import io.zonarosa.server.storage.Account;
import io.zonarosa.server.storage.AccountsManager;
import io.zonarosa.server.storage.DeviceCapability;
import io.zonarosa.server.storage.ProfilesManager;
import io.zonarosa.server.storage.VersionedProfile;
import io.zonarosa.server.tests.util.AuthHelper;
import io.zonarosa.server.tests.util.ProfileTestHelper;
import io.zonarosa.server.util.TestRandomUtil;
import io.zonarosa.server.util.UUIDUtil;

public class ProfileAnonymousGrpcServiceTest extends SimpleBaseGrpcTest<ProfileAnonymousGrpcService, ProfileAnonymousGrpc.ProfileAnonymousBlockingStub> {

  private final ServerSecretParams SERVER_SECRET_PARAMS = ServerSecretParams.generate();

  @Mock
  private Account account;

  @Mock
  private AccountsManager accountsManager;

  @Mock
  private ProfilesManager profilesManager;

  @Mock
  private ProfileBadgeConverter profileBadgeConverter;

  @Override
  protected ProfileAnonymousGrpcService createServiceBeforeEachTest() {
    getMockRequestAttributesInterceptor().setRequestAttributes(new RequestAttributes(InetAddresses.forString("127.0.0.1"),
        "ZonaRosa-Android/1.2.3",
        Locale.LanguageRange.parse("en-us")));

    return new ProfileAnonymousGrpcService(
        accountsManager,
        profilesManager,
        profileBadgeConverter,
        SERVER_SECRET_PARAMS
    );
  }

  @Test
  void getUnversionedProfileUnidentifiedAccessKey() {
    final UUID targetUuid = UUID.randomUUID();
    final io.zonarosa.server.identity.ServiceIdentifier serviceIdentifier = new AciServiceIdentifier(targetUuid);

    final byte[] unidentifiedAccessKey = TestRandomUtil.nextBytes(UnidentifiedAccessUtil.UNIDENTIFIED_ACCESS_KEY_LENGTH);
    final ECKeyPair identityKeyPair = ECKeyPair.generate();
    final IdentityKey identityKey = new IdentityKey(identityKeyPair.getPublicKey());

    final List<Badge> badges = List.of(new Badge(
        "TEST",
        "other",
        "Test Badge",
        "This badge is in unit tests.",
        List.of("l", "m", "h", "x", "xx", "xxx"),
        "SVG",
        List.of(
            new BadgeSvg("sl", "sd"),
            new BadgeSvg("ml", "md"),
            new BadgeSvg("ll", "ld")))
    );

    when(account.getBadges()).thenReturn(Collections.emptyList());
    when(profileBadgeConverter.convert(any(), any(), anyBoolean())).thenReturn(badges);
    when(account.isUnrestrictedUnidentifiedAccess()).thenReturn(false);
    when(account.getUnidentifiedAccessKey()).thenReturn(Optional.of(unidentifiedAccessKey));
    when(account.getIdentityKey(io.zonarosa.server.identity.IdentityType.ACI)).thenReturn(identityKey);
    when(account.hasCapability(any())).thenReturn(false);
    when(accountsManager.getByServiceIdentifier(serviceIdentifier)).thenReturn(Optional.of(account));

    final GetUnversionedProfileAnonymousRequest request = GetUnversionedProfileAnonymousRequest.newBuilder()
        .setUnidentifiedAccessKey(ByteString.copyFrom(unidentifiedAccessKey))
        .setRequest(GetUnversionedProfileRequest.newBuilder()
            .setServiceIdentifier(ServiceIdentifier.newBuilder()
                .setIdentityType(IdentityType.IDENTITY_TYPE_ACI)
                .setUuid(ByteString.copyFrom(UUIDUtil.toBytes(targetUuid)))
                .build())
            .build())
        .build();

    final GetUnversionedProfileResponse response = unauthenticatedServiceStub().getUnversionedProfile(request);

    final byte[] unidentifiedAccessChecksum = UnidentifiedAccessChecksum.generateFor(unidentifiedAccessKey);
    final GetUnversionedProfileResponse expectedResponse = GetUnversionedProfileResponse.newBuilder()
        .setIdentityKey(ByteString.copyFrom(identityKey.serialize()))
        .setUnidentifiedAccess(ByteString.copyFrom(unidentifiedAccessChecksum))
        .setUnrestrictedUnidentifiedAccess(false)
        .addAllBadges(ProfileGrpcHelper.buildBadges(badges))
        .build();

    verify(accountsManager).getByServiceIdentifier(serviceIdentifier);
    assertEquals(expectedResponse, response);
  }

  @Test
  void getUnversionedProfileGroupSendEndorsement() throws Exception {
    final UUID targetUuid = UUID.randomUUID();
    final io.zonarosa.server.identity.ServiceIdentifier serviceIdentifier = new AciServiceIdentifier(targetUuid);

    // Expiration must be on a day boundary; we want one in the future
    final Instant expiration = Instant.now().plus(Duration.ofDays(1)).truncatedTo(ChronoUnit.DAYS);
    final byte[] token = AuthHelper.validGroupSendToken(SERVER_SECRET_PARAMS, List.of(serviceIdentifier), expiration);

    final ECKeyPair identityKeyPair = ECKeyPair.generate();
    final IdentityKey identityKey = new IdentityKey(identityKeyPair.getPublicKey());

    final List<Badge> badges = List.of(new Badge(
        "TEST",
        "other",
        "Test Badge",
        "This badge is in unit tests.",
        List.of("l", "m", "h", "x", "xx", "xxx"),
        "SVG",
        List.of(
            new BadgeSvg("sl", "sd"),
            new BadgeSvg("ml", "md"),
            new BadgeSvg("ll", "ld")))
    );

    when(account.getBadges()).thenReturn(Collections.emptyList());
    when(profileBadgeConverter.convert(any(), any(), anyBoolean())).thenReturn(badges);
    when(account.isUnrestrictedUnidentifiedAccess()).thenReturn(false);
    when(account.getIdentityKey(io.zonarosa.server.identity.IdentityType.ACI)).thenReturn(identityKey);
    when(accountsManager.getByServiceIdentifier(serviceIdentifier)).thenReturn(Optional.of(account));

    final GetUnversionedProfileAnonymousRequest request = GetUnversionedProfileAnonymousRequest.newBuilder()
        .setGroupSendToken(ByteString.copyFrom(token))
        .setRequest(GetUnversionedProfileRequest.newBuilder()
            .setServiceIdentifier(
                ServiceIdentifierUtil.toGrpcServiceIdentifier(serviceIdentifier))
            .build())
        .build();

    final GetUnversionedProfileResponse response = unauthenticatedServiceStub().getUnversionedProfile(request);

    final GetUnversionedProfileResponse expectedResponse = GetUnversionedProfileResponse.newBuilder()
        .setIdentityKey(ByteString.copyFrom(identityKey.serialize()))
        .setUnrestrictedUnidentifiedAccess(false)
        .addAllCapabilities(ProfileGrpcHelper.buildAccountCapabilities(account))
        .addAllBadges(ProfileGrpcHelper.buildBadges(badges))
        .build();

    verify(accountsManager).getByServiceIdentifier(serviceIdentifier);
    assertEquals(expectedResponse, response);
  }

  @Test
  void getUnversionedProfileNoAuth() {
    final GetUnversionedProfileAnonymousRequest request = GetUnversionedProfileAnonymousRequest.newBuilder()
        .setRequest(GetUnversionedProfileRequest.newBuilder()
            .setServiceIdentifier(ServiceIdentifierUtil.toGrpcServiceIdentifier(new AciServiceIdentifier(UUID.randomUUID()))))
        .build();

    assertStatusException(Status.INVALID_ARGUMENT, () -> unauthenticatedServiceStub().getUnversionedProfile(request));
  }

  @ParameterizedTest
  @MethodSource
  void getUnversionedProfileIncorrectUnidentifiedAccessKey(final IdentityType identityType, final boolean wrongUnidentifiedAccessKey, final boolean accountNotFound) {
    final byte[] unidentifiedAccessKey = TestRandomUtil.nextBytes(UnidentifiedAccessUtil.UNIDENTIFIED_ACCESS_KEY_LENGTH);

    when(account.getUnidentifiedAccessKey()).thenReturn(Optional.of(unidentifiedAccessKey));
    when(account.isUnrestrictedUnidentifiedAccess()).thenReturn(false);
    when(accountsManager.getByServiceIdentifier(any())).thenReturn(
        accountNotFound ? Optional.empty() : Optional.of(account));

    final GetUnversionedProfileAnonymousRequest request = GetUnversionedProfileAnonymousRequest.newBuilder()
        .setUnidentifiedAccessKey(
            ByteString.copyFrom(wrongUnidentifiedAccessKey
                ? new byte[UnidentifiedAccessUtil.UNIDENTIFIED_ACCESS_KEY_LENGTH]
                : unidentifiedAccessKey))
        .setRequest(GetUnversionedProfileRequest.newBuilder()
            .setServiceIdentifier(ServiceIdentifier.newBuilder()
                .setIdentityType(identityType)
                .setUuid(ByteString.copyFrom(UUIDUtil.toBytes(UUID.randomUUID())))))
        .build();

    assertStatusException(Status.UNAUTHENTICATED, () -> unauthenticatedServiceStub().getUnversionedProfile(request));
  }

  private static Stream<Arguments> getUnversionedProfileIncorrectUnidentifiedAccessKey() {
    return Stream.of(
        Arguments.of(IdentityType.IDENTITY_TYPE_PNI, false, false),
        Arguments.of(IdentityType.IDENTITY_TYPE_ACI, true, false),
        Arguments.of(IdentityType.IDENTITY_TYPE_ACI, false, true)
    );
  }

  @Test
  void getUnversionedProfileExpiredGroupSendEndorsement() throws Exception {
    final AciServiceIdentifier serviceIdentifier = new AciServiceIdentifier(UUID.randomUUID());
    // Expirations must be on a day boundary; pick one in the recent past
    final Instant expiration = Instant.now().truncatedTo(ChronoUnit.DAYS);
    final byte[] token = AuthHelper.validGroupSendToken(SERVER_SECRET_PARAMS, List.of(serviceIdentifier), expiration);

    final GetUnversionedProfileAnonymousRequest request = GetUnversionedProfileAnonymousRequest.newBuilder()
        .setGroupSendToken(ByteString.copyFrom(token))
        .setRequest(GetUnversionedProfileRequest.newBuilder()
            .setServiceIdentifier(
                ServiceIdentifierUtil.toGrpcServiceIdentifier(serviceIdentifier)))
        .build();

    assertStatusException(Status.UNAUTHENTICATED, () -> unauthenticatedServiceStub().getUnversionedProfile(request));
  }

  @Test
  void getUnversionedProfileIncorrectGroupSendEndorsement() throws Exception {
    final AciServiceIdentifier targetServiceIdentifier = new AciServiceIdentifier(UUID.randomUUID());
    final AciServiceIdentifier authorizedServiceIdentifier = new AciServiceIdentifier(UUID.randomUUID());

    // Expiration must be on a day boundary; we want one in the future
    final Instant expiration = Instant.now().plus(Duration.ofDays(1)).truncatedTo(ChronoUnit.DAYS);
    final byte[] token = AuthHelper.validGroupSendToken(SERVER_SECRET_PARAMS, List.of(authorizedServiceIdentifier), expiration);

    when(accountsManager.getByServiceIdentifier(any())).thenReturn(
        Optional.empty());
    final GetUnversionedProfileAnonymousRequest request = GetUnversionedProfileAnonymousRequest.newBuilder()
        .setGroupSendToken(ByteString.copyFrom(token))
        .setRequest(GetUnversionedProfileRequest.newBuilder()
            .setServiceIdentifier(
                ServiceIdentifierUtil.toGrpcServiceIdentifier(targetServiceIdentifier)))
        .build();

    assertStatusException(Status.UNAUTHENTICATED, () -> unauthenticatedServiceStub().getUnversionedProfile(request));
  }

  @Test
  void getUnversionedProfileGroupSendEndorsementAccountNotFound() throws Exception {
    final AciServiceIdentifier serviceIdentifier = new AciServiceIdentifier(UUID.randomUUID());

    // Expiration must be on a day boundary; we want one in the future
    final Instant expiration = Instant.now().plus(Duration.ofDays(1)).truncatedTo(ChronoUnit.DAYS);
    final byte[] token = AuthHelper.validGroupSendToken(SERVER_SECRET_PARAMS, List.of(serviceIdentifier), expiration);

    when(accountsManager.getByServiceIdentifier(any())).thenReturn(Optional.empty());
    final GetUnversionedProfileAnonymousRequest request = GetUnversionedProfileAnonymousRequest.newBuilder()
        .setGroupSendToken(ByteString.copyFrom(token))
        .setRequest(GetUnversionedProfileRequest.newBuilder()
            .setServiceIdentifier(ServiceIdentifierUtil.toGrpcServiceIdentifier(serviceIdentifier)))
        .build();

    assertStatusException(Status.NOT_FOUND, () -> unauthenticatedServiceStub().getUnversionedProfile(request));
  }

  @ParameterizedTest
  @MethodSource
  void getVersionedProfile(final String requestVersion,
      @Nullable final String accountVersion,
      final boolean expectResponseHasPaymentAddress) {
    final byte[] unidentifiedAccessKey = TestRandomUtil.nextBytes(UnidentifiedAccessUtil.UNIDENTIFIED_ACCESS_KEY_LENGTH);

    final byte[] name = TestRandomUtil.nextBytes(81);
    final byte[] emoji = TestRandomUtil.nextBytes(60);
    final byte[] about = TestRandomUtil.nextBytes(156);
    final byte[] paymentAddress = TestRandomUtil.nextBytes(582);
    final byte[] phoneNumberSharing = TestRandomUtil.nextBytes(29);
    final String avatar = "profiles/" + ProfileTestHelper.generateRandomBase64FromByteArray(16);

    final VersionedProfile profile = new VersionedProfile(accountVersion, name, avatar, emoji, about, paymentAddress, phoneNumberSharing, new byte[0]);

    when(account.getCurrentProfileVersion()).thenReturn(Optional.ofNullable(accountVersion));
    when(account.isUnrestrictedUnidentifiedAccess()).thenReturn(false);
    when(account.getUnidentifiedAccessKey()).thenReturn(Optional.of(unidentifiedAccessKey));

    when(accountsManager.getByServiceIdentifier(any())).thenReturn(Optional.of(account));
    when(profilesManager.get(any(), any())).thenReturn(Optional.of(profile));

    final GetVersionedProfileAnonymousRequest request = GetVersionedProfileAnonymousRequest.newBuilder()
        .setUnidentifiedAccessKey(ByteString.copyFrom(unidentifiedAccessKey))
        .setRequest(GetVersionedProfileRequest.newBuilder()
            .setAccountIdentifier(ServiceIdentifier.newBuilder()
                .setIdentityType(IdentityType.IDENTITY_TYPE_ACI)
                .setUuid(ByteString.copyFrom(UUIDUtil.toBytes(UUID.randomUUID())))
                .build())
            .setVersion(requestVersion)
            .build())
        .build();

    final GetVersionedProfileResponse response = unauthenticatedServiceStub().getVersionedProfile(request);

    final GetVersionedProfileResponse.Builder expectedResponseBuilder = GetVersionedProfileResponse.newBuilder()
        .setName(ByteString.copyFrom(name))
        .setAbout(ByteString.copyFrom(about))
        .setAboutEmoji(ByteString.copyFrom(emoji))
        .setAvatar(avatar)
        .setPhoneNumberSharing(ByteString.copyFrom(phoneNumberSharing));

    if (expectResponseHasPaymentAddress) {
      expectedResponseBuilder.setPaymentAddress(ByteString.copyFrom(paymentAddress));
    }

    assertEquals(expectedResponseBuilder.build(), response);
  }

  private static Stream<Arguments> getVersionedProfile() {
    return Stream.of(
        Arguments.of("version1", "version1", true),
        Arguments.of("version1", null, true),
        Arguments.of("version1", "version2", false)
    );
  }

  @Test
  void getVersionedProfileVersionNotFound() {
    final byte[] unidentifiedAccessKey = TestRandomUtil.nextBytes(UnidentifiedAccessUtil.UNIDENTIFIED_ACCESS_KEY_LENGTH);

    when(account.getUnidentifiedAccessKey()).thenReturn(Optional.of(unidentifiedAccessKey));
    when(account.isUnrestrictedUnidentifiedAccess()).thenReturn(false);

    when(accountsManager.getByServiceIdentifier(any())).thenReturn(Optional.of(account));
    when(profilesManager.get(any(), any())).thenReturn(Optional.empty());

    final GetVersionedProfileAnonymousRequest request = GetVersionedProfileAnonymousRequest.newBuilder()
        .setUnidentifiedAccessKey(ByteString.copyFrom(unidentifiedAccessKey))
        .setRequest(GetVersionedProfileRequest.newBuilder()
            .setAccountIdentifier(ServiceIdentifier.newBuilder()
                .setIdentityType(IdentityType.IDENTITY_TYPE_ACI)
                .setUuid(ByteString.copyFrom(UUIDUtil.toBytes(UUID.randomUUID())))
                .build())
            .setVersion("someVersion")
            .build())
        .build();

    assertStatusException(Status.NOT_FOUND, () -> unauthenticatedServiceStub().getVersionedProfile(request));
  }

  @ParameterizedTest
  @MethodSource
  void getVersionedProfileUnauthenticated(final boolean missingUnidentifiedAccessKey,
      final boolean accountNotFound) {
    final byte[] unidentifiedAccessKey = TestRandomUtil.nextBytes(UnidentifiedAccessUtil.UNIDENTIFIED_ACCESS_KEY_LENGTH);

    when(account.isUnrestrictedUnidentifiedAccess()).thenReturn(false);
    when(account.getUnidentifiedAccessKey()).thenReturn(Optional.of(unidentifiedAccessKey));
    when(accountsManager.getByServiceIdentifier(any())).thenReturn(
        accountNotFound ? Optional.empty() : Optional.of(account));

    final GetVersionedProfileAnonymousRequest.Builder requestBuilder = GetVersionedProfileAnonymousRequest.newBuilder()
        .setRequest(GetVersionedProfileRequest.newBuilder()
            .setAccountIdentifier(ServiceIdentifier.newBuilder()
                .setIdentityType(IdentityType.IDENTITY_TYPE_ACI)
                .setUuid(ByteString.copyFrom(UUIDUtil.toBytes(UUID.randomUUID())))
                .build())
            .setVersion("someVersion")
            .build());

    if (!missingUnidentifiedAccessKey) {
      requestBuilder.setUnidentifiedAccessKey(ByteString.copyFrom(unidentifiedAccessKey));
    }

    assertStatusException(Status.UNAUTHENTICATED, () -> unauthenticatedServiceStub().getVersionedProfile(requestBuilder.build()));
  }
  private static Stream<Arguments> getVersionedProfileUnauthenticated() {
    return Stream.of(
        Arguments.of(true, false),
        Arguments.of(false, true)
    );
  }

  @Test
  void getVersionedProfilePniInvalidArgument() {
    final byte[] unidentifiedAccessKey = TestRandomUtil.nextBytes(UnidentifiedAccessUtil.UNIDENTIFIED_ACCESS_KEY_LENGTH);

    final GetVersionedProfileAnonymousRequest request = GetVersionedProfileAnonymousRequest.newBuilder()
        .setUnidentifiedAccessKey(ByteString.copyFrom(unidentifiedAccessKey))
        .setRequest(GetVersionedProfileRequest.newBuilder()
            .setAccountIdentifier(ServiceIdentifier.newBuilder()
                .setIdentityType(IdentityType.IDENTITY_TYPE_PNI)
                .setUuid(ByteString.copyFrom(UUIDUtil.toBytes(UUID.randomUUID())))
                .build())
            .setVersion("someVersion")
            .build())
        .build();

    assertStatusException(Status.INVALID_ARGUMENT, () -> unauthenticatedServiceStub().getVersionedProfile(request));
  }

  @Test
  void getExpiringProfileKeyCredential() throws InvalidInputException, VerificationFailedException {
    final byte[] unidentifiedAccessKey = TestRandomUtil.nextBytes(UnidentifiedAccessUtil.UNIDENTIFIED_ACCESS_KEY_LENGTH);
    final UUID targetUuid = UUID.randomUUID();

    final ClientZkProfileOperations clientZkProfile = new ClientZkProfileOperations(SERVER_SECRET_PARAMS.getPublicParams());

    final byte[] profileKeyBytes = TestRandomUtil.nextBytes(32);
    final ProfileKey profileKey = new ProfileKey(profileKeyBytes);
    final ProfileKeyCommitment profileKeyCommitment = profileKey.getCommitment(new ServiceId.Aci(targetUuid));
    final ProfileKeyCredentialRequestContext profileKeyCredentialRequestContext =
        clientZkProfile.createProfileKeyCredentialRequestContext(new ServiceId.Aci(targetUuid), profileKey);

    final VersionedProfile profile = mock(VersionedProfile.class);
    when(profile.commitment()).thenReturn(profileKeyCommitment.serialize());

    when(account.getUuid()).thenReturn(targetUuid);
    when(account.getUnidentifiedAccessKey()).thenReturn(Optional.of(unidentifiedAccessKey));
    when(accountsManager.getByServiceIdentifier(new AciServiceIdentifier(targetUuid))).thenReturn(Optional.of(account));
    when(profilesManager.get(targetUuid, "someVersion")).thenReturn(Optional.of(profile));

    final ProfileKeyCredentialRequest credentialRequest = profileKeyCredentialRequestContext.getRequest();

    final Instant expiration = Instant.now().plus(io.zonarosa.server.util.ProfileHelper.EXPIRING_PROFILE_KEY_CREDENTIAL_EXPIRATION)
        .truncatedTo(ChronoUnit.DAYS);

    final GetExpiringProfileKeyCredentialAnonymousRequest request = GetExpiringProfileKeyCredentialAnonymousRequest.newBuilder()
        .setRequest(GetExpiringProfileKeyCredentialRequest.newBuilder()
            .setAccountIdentifier(ServiceIdentifier.newBuilder()
                .setIdentityType(IdentityType.IDENTITY_TYPE_ACI)
                .setUuid(ByteString.copyFrom(UUIDUtil.toBytes(targetUuid)))
                .build())
            .setCredentialRequest(ByteString.copyFrom(credentialRequest.serialize()))
            .setCredentialType(CredentialType.CREDENTIAL_TYPE_EXPIRING_PROFILE_KEY)
            .setVersion("someVersion")
            .build())
        .setUnidentifiedAccessKey(ByteString.copyFrom(unidentifiedAccessKey))
        .build();

    final GetExpiringProfileKeyCredentialResponse response = unauthenticatedServiceStub().getExpiringProfileKeyCredential(request);

    assertThatNoException().isThrownBy(() ->
        clientZkProfile.receiveExpiringProfileKeyCredential(profileKeyCredentialRequestContext, new ExpiringProfileKeyCredentialResponse(response.getProfileKeyCredential().toByteArray())));
  }

  @ParameterizedTest
  @MethodSource
  void getExpiringProfileKeyCredentialUnauthenticated(final boolean missingAccount, final boolean missingUnidentifiedAccessKey) {
    final byte[] unidentifiedAccessKey = TestRandomUtil.nextBytes(UnidentifiedAccessUtil.UNIDENTIFIED_ACCESS_KEY_LENGTH);
    final UUID targetUuid = UUID.randomUUID();

    when(account.getUuid()).thenReturn(targetUuid);
    when(account.getUnidentifiedAccessKey()).thenReturn(Optional.of(unidentifiedAccessKey));
    when(accountsManager.getByServiceIdentifier(new AciServiceIdentifier(targetUuid))).thenReturn(
        missingAccount ? Optional.empty() : Optional.of(account));

    final GetExpiringProfileKeyCredentialAnonymousRequest.Builder requestBuilder = GetExpiringProfileKeyCredentialAnonymousRequest.newBuilder()
        .setRequest(GetExpiringProfileKeyCredentialRequest.newBuilder()
            .setAccountIdentifier(ServiceIdentifier.newBuilder()
                .setIdentityType(IdentityType.IDENTITY_TYPE_ACI)
                .setUuid(ByteString.copyFrom(UUIDUtil.toBytes(targetUuid)))
                .build())
            .setCredentialRequest(ByteString.copyFrom("credentialRequest".getBytes(StandardCharsets.UTF_8)))
            .setCredentialType(CredentialType.CREDENTIAL_TYPE_EXPIRING_PROFILE_KEY)
            .setVersion("someVersion")
            .build());

    if (!missingUnidentifiedAccessKey) {
      requestBuilder.setUnidentifiedAccessKey(ByteString.copyFrom(unidentifiedAccessKey));
    }

    assertStatusException(Status.UNAUTHENTICATED, () -> unauthenticatedServiceStub().getExpiringProfileKeyCredential(requestBuilder.build()));

    verifyNoInteractions(profilesManager);
  }

  private static Stream<Arguments> getExpiringProfileKeyCredentialUnauthenticated() {
    return Stream.of(
        Arguments.of(true, false),
        Arguments.of(false, true)
    );
  }


  @Test
  void getExpiringProfileKeyCredentialProfileNotFound() {
    final byte[] unidentifiedAccessKey = TestRandomUtil.nextBytes(UnidentifiedAccessUtil.UNIDENTIFIED_ACCESS_KEY_LENGTH);
    final UUID targetUuid = UUID.randomUUID();

    when(account.getUuid()).thenReturn(targetUuid);
    when(account.getUnidentifiedAccessKey()).thenReturn(Optional.of(unidentifiedAccessKey));
    when(accountsManager.getByServiceIdentifier(new AciServiceIdentifier(targetUuid))).thenReturn(
        Optional.of(account));
    when(profilesManager.get(targetUuid, "someVersion")).thenReturn(Optional.empty());

    final GetExpiringProfileKeyCredentialAnonymousRequest request = GetExpiringProfileKeyCredentialAnonymousRequest.newBuilder()
        .setUnidentifiedAccessKey(ByteString.copyFrom(unidentifiedAccessKey))
        .setRequest(GetExpiringProfileKeyCredentialRequest.newBuilder()
            .setAccountIdentifier(ServiceIdentifier.newBuilder()
                .setIdentityType(IdentityType.IDENTITY_TYPE_ACI)
                .setUuid(ByteString.copyFrom(UUIDUtil.toBytes(targetUuid)))
                .build())
            .setCredentialRequest(ByteString.copyFrom("credentialRequest".getBytes(StandardCharsets.UTF_8)))
            .setCredentialType(CredentialType.CREDENTIAL_TYPE_EXPIRING_PROFILE_KEY)
            .setVersion("someVersion")
            .build())
        .build();

    assertStatusException(Status.NOT_FOUND, () -> unauthenticatedServiceStub().getExpiringProfileKeyCredential(request));
  }

  @ParameterizedTest
  @MethodSource
  void getExpiringProfileKeyCredentialInvalidArgument(final IdentityType identityType, final CredentialType credentialType,
      final boolean throwZkVerificationException) throws VerificationFailedException {
    final UUID targetUuid = UUID.randomUUID();
    final byte[] unidentifiedAccessKey = TestRandomUtil.nextBytes(UnidentifiedAccessUtil.UNIDENTIFIED_ACCESS_KEY_LENGTH);

    final VersionedProfile profile = mock(VersionedProfile.class);
    when(profile.commitment()).thenReturn("commitment".getBytes(StandardCharsets.UTF_8));
    when(account.getUuid()).thenReturn(targetUuid);
    when(account.getUnidentifiedAccessKey()).thenReturn(Optional.of(unidentifiedAccessKey));
    when(accountsManager.getByServiceIdentifier(new AciServiceIdentifier(targetUuid))).thenReturn(Optional.of(account));
    when(profilesManager.get(targetUuid, "someVersion")).thenReturn(Optional.of(profile));

    final GetExpiringProfileKeyCredentialAnonymousRequest request = GetExpiringProfileKeyCredentialAnonymousRequest.newBuilder()
        .setUnidentifiedAccessKey(ByteString.copyFrom(unidentifiedAccessKey))
        .setRequest(GetExpiringProfileKeyCredentialRequest.newBuilder()
            .setAccountIdentifier(ServiceIdentifier.newBuilder()
                .setIdentityType(identityType)
                .setUuid(ByteString.copyFrom(UUIDUtil.toBytes(targetUuid)))
                .build())
            .setCredentialRequest(ByteString.copyFrom("credentialRequest".getBytes(StandardCharsets.UTF_8)))
            .setCredentialType(credentialType)
            .setVersion("someVersion")
            .build())
        .build();

    assertStatusException(Status.INVALID_ARGUMENT, () -> unauthenticatedServiceStub().getExpiringProfileKeyCredential(request));
  }

  private static Stream<Arguments> getExpiringProfileKeyCredentialInvalidArgument() {
    return Stream.of(
        // Credential type unspecified
        Arguments.of(IdentityType.IDENTITY_TYPE_ACI, CredentialType.CREDENTIAL_TYPE_UNSPECIFIED, false),
        // Illegal identity type
        Arguments.of(IdentityType.IDENTITY_TYPE_PNI, CredentialType.CREDENTIAL_TYPE_EXPIRING_PROFILE_KEY, false),
        // Artificially fails zero knowledge verification
        Arguments.of(IdentityType.IDENTITY_TYPE_ACI, CredentialType.CREDENTIAL_TYPE_EXPIRING_PROFILE_KEY, true)
    );
  }

  @Override
  protected List<ServerInterceptor> customizeInterceptors(List<ServerInterceptor> serverInterceptors) {
    return serverInterceptors.stream()
        // For now, don't validate error conformance because the profiles gRPC service has not been converted to the
        // updated error model
        .filter(interceptor -> !(interceptor instanceof ErrorConformanceInterceptor))
        .toList();
  }
}
