/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.grpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyByte;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static io.zonarosa.server.grpc.GrpcTestUtils.assertStatusException;

import com.google.protobuf.ByteString;
import io.grpc.Status;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import io.zonarosa.chat.common.EcPreKey;
import io.zonarosa.chat.common.EcSignedPreKey;
import io.zonarosa.chat.common.KemSignedPreKey;
import io.zonarosa.chat.common.ServiceIdentifier;
import io.zonarosa.chat.keys.AccountPreKeyBundles;
import io.zonarosa.chat.keys.CheckIdentityKeyRequest;
import io.zonarosa.chat.keys.DevicePreKeyBundle;
import io.zonarosa.chat.keys.GetPreKeysAnonymousRequest;
import io.zonarosa.chat.keys.GetPreKeysAnonymousResponse;
import io.zonarosa.chat.keys.GetPreKeysRequest;
import io.zonarosa.chat.keys.KeysAnonymousGrpc;
import io.zonarosa.chat.keys.ReactorKeysAnonymousGrpc;
import io.zonarosa.libzonarosa.protocol.IdentityKey;
import io.zonarosa.libzonarosa.protocol.InvalidKeyException;
import io.zonarosa.libzonarosa.protocol.ecc.ECKeyPair;
import io.zonarosa.libzonarosa.zkgroup.ServerSecretParams;
import io.zonarosa.server.auth.UnidentifiedAccessUtil;
import io.zonarosa.server.entities.ECPreKey;
import io.zonarosa.server.entities.ECSignedPreKey;
import io.zonarosa.server.entities.KEMSignedPreKey;
import io.zonarosa.server.identity.AciServiceIdentifier;
import io.zonarosa.server.identity.IdentityType;
import io.zonarosa.server.identity.PniServiceIdentifier;
import io.zonarosa.server.storage.Account;
import io.zonarosa.server.storage.AccountsManager;
import io.zonarosa.server.storage.Device;
import io.zonarosa.server.storage.KeysManager;
import io.zonarosa.server.tests.util.AuthHelper;
import io.zonarosa.server.tests.util.DevicesHelper;
import io.zonarosa.server.tests.util.KeysHelper;
import io.zonarosa.server.util.TestClock;
import io.zonarosa.server.util.TestRandomUtil;
import io.zonarosa.server.util.UUIDUtil;
import io.zonarosa.server.util.Util;
import reactor.core.publisher.Flux;

class KeysAnonymousGrpcServiceTest extends SimpleBaseGrpcTest<KeysAnonymousGrpcService, KeysAnonymousGrpc.KeysAnonymousBlockingStub> {

  private static final ServerSecretParams SERVER_SECRET_PARAMS = ServerSecretParams.generate();
  private static final TestClock CLOCK = TestClock.now();

  @Mock
  private AccountsManager accountsManager;

  @Mock
  private KeysManager keysManager;

  @Override
  protected KeysAnonymousGrpcService createServiceBeforeEachTest() {
    return new KeysAnonymousGrpcService(accountsManager, keysManager, SERVER_SECRET_PARAMS, CLOCK);
  }

  @Test
  void getPreKeysUnidentifiedAccessKey() {
    final Account targetAccount = mock(Account.class);

    final Device targetDevice = DevicesHelper.createDevice(Device.PRIMARY_ID);
    when(targetAccount.getDevice(Device.PRIMARY_ID)).thenReturn(Optional.of(targetDevice));

    final ECKeyPair identityKeyPair = ECKeyPair.generate();
    final IdentityKey identityKey = new IdentityKey(identityKeyPair.getPublicKey());
    final UUID uuid = UUID.randomUUID();
    final AciServiceIdentifier identifier = new AciServiceIdentifier(uuid);
    final byte[] unidentifiedAccessKey = TestRandomUtil.nextBytes(UnidentifiedAccessUtil.UNIDENTIFIED_ACCESS_KEY_LENGTH);

    when(targetAccount.getUnidentifiedAccessKey()).thenReturn(Optional.of(unidentifiedAccessKey));
    when(targetAccount.getIdentifier(IdentityType.ACI)).thenReturn(uuid);
    when(targetAccount.getIdentityKey(IdentityType.ACI)).thenReturn(identityKey);
    when(accountsManager.getByServiceIdentifierAsync(identifier))
        .thenReturn(CompletableFuture.completedFuture(Optional.of(targetAccount)));

    final ECPreKey ecPreKey = new ECPreKey(1, ECKeyPair.generate().getPublicKey());
    final ECSignedPreKey ecSignedPreKey = KeysHelper.signedECPreKey(2, identityKeyPair);
    final KEMSignedPreKey kemSignedPreKey = KeysHelper.signedKEMPreKey(3, identityKeyPair);
    final KeysManager.DevicePreKeys devicePreKeys =
        new KeysManager.DevicePreKeys(ecSignedPreKey, Optional.of(ecPreKey), kemSignedPreKey);

    when(keysManager.takeDevicePreKeys(eq(Device.PRIMARY_ID), eq(identifier), any()))
        .thenReturn(CompletableFuture.completedFuture(Optional.of(devicePreKeys)));

    final GetPreKeysAnonymousResponse response = unauthenticatedServiceStub().getPreKeys(GetPreKeysAnonymousRequest.newBuilder()
        .setUnidentifiedAccessKey(ByteString.copyFrom(unidentifiedAccessKey))
        .setRequest(GetPreKeysRequest.newBuilder()
            .setTargetIdentifier(ServiceIdentifierUtil.toGrpcServiceIdentifier(identifier))
            .setDeviceId(Device.PRIMARY_ID))
        .build());

    final GetPreKeysAnonymousResponse expectedResponse = GetPreKeysAnonymousResponse.newBuilder()
        .setPreKeys(AccountPreKeyBundles.newBuilder()
            .setIdentityKey(ByteString.copyFrom(identityKey.serialize()))
            .putDevicePreKeys(Device.PRIMARY_ID, DevicePreKeyBundle.newBuilder()
                .setEcOneTimePreKey(toGrpcEcPreKey(ecPreKey))
                .setEcSignedPreKey(toGrpcEcSignedPreKey(ecSignedPreKey))
                .setKemOneTimePreKey(toGrpcKemSignedPreKey(kemSignedPreKey))
                .build()))
        .build();

    assertEquals(expectedResponse, response);
  }

  @Test
  void getPreKeysGroupSendEndorsement() throws Exception {
    final Account targetAccount = mock(Account.class);

    final Device targetDevice = DevicesHelper.createDevice(Device.PRIMARY_ID);
    when(targetAccount.getDevice(Device.PRIMARY_ID)).thenReturn(Optional.of(targetDevice));

    final ECKeyPair identityKeyPair = ECKeyPair.generate();
    final IdentityKey identityKey = new IdentityKey(identityKeyPair.getPublicKey());
    final UUID uuid = UUID.randomUUID();
    final AciServiceIdentifier identifier = new AciServiceIdentifier(uuid);
    final byte[] unidentifiedAccessKey = TestRandomUtil.nextBytes(UnidentifiedAccessUtil.UNIDENTIFIED_ACCESS_KEY_LENGTH);

    when(targetAccount.getUnidentifiedAccessKey()).thenReturn(Optional.of(unidentifiedAccessKey));
    when(targetAccount.getIdentifier(IdentityType.ACI)).thenReturn(uuid);
    when(targetAccount.getIdentityKey(IdentityType.ACI)).thenReturn(identityKey);
    when(accountsManager.getByServiceIdentifierAsync(identifier))
        .thenReturn(CompletableFuture.completedFuture(Optional.of(targetAccount)));

    final ECPreKey ecPreKey = new ECPreKey(1, ECKeyPair.generate().getPublicKey());
    final ECSignedPreKey ecSignedPreKey = KeysHelper.signedECPreKey(2, identityKeyPair);
    final KEMSignedPreKey kemSignedPreKey = KeysHelper.signedKEMPreKey(3, identityKeyPair);
    final KeysManager.DevicePreKeys devicePreKeys =
        new KeysManager.DevicePreKeys(ecSignedPreKey, Optional.of(ecPreKey), kemSignedPreKey);

    when(keysManager.takeDevicePreKeys(eq(Device.PRIMARY_ID), eq(identifier), any()))
        .thenReturn(CompletableFuture.completedFuture(Optional.of(devicePreKeys)));

    // Expirations must be on day boundaries or libzonarosa will refuse to create or verify the token
    final Instant expiration = Instant.now().truncatedTo(ChronoUnit.DAYS);
    CLOCK.pin(expiration.minus(Duration.ofHours(1))); // set time so the credential isn't expired yet
    final byte[] token = AuthHelper.validGroupSendToken(SERVER_SECRET_PARAMS, List.of(identifier), expiration);

    final GetPreKeysAnonymousResponse response = unauthenticatedServiceStub().getPreKeys(GetPreKeysAnonymousRequest.newBuilder()
        .setGroupSendToken(ByteString.copyFrom(token))
        .setRequest(GetPreKeysRequest.newBuilder()
            .setTargetIdentifier(ServiceIdentifierUtil.toGrpcServiceIdentifier(identifier))
            .setDeviceId(Device.PRIMARY_ID))
        .build());

    final GetPreKeysAnonymousResponse expectedResponse = GetPreKeysAnonymousResponse.newBuilder()
        .setPreKeys(AccountPreKeyBundles.newBuilder()
            .setIdentityKey(ByteString.copyFrom(identityKey.serialize()))
            .putDevicePreKeys(Device.PRIMARY_ID, DevicePreKeyBundle.newBuilder()
                .setEcOneTimePreKey(toGrpcEcPreKey(ecPreKey))
                .setEcSignedPreKey(toGrpcEcSignedPreKey(ecSignedPreKey))
                .setKemOneTimePreKey(toGrpcKemSignedPreKey(kemSignedPreKey))
                .build()))
        .build();

    assertEquals(expectedResponse, response);
  }

  @Test
  void getPreKeysNoAuth() {
    assertGetKeysFailure(Status.INVALID_ARGUMENT, GetPreKeysAnonymousRequest.newBuilder()
        .setRequest(GetPreKeysRequest.newBuilder()
            .setTargetIdentifier(ServiceIdentifierUtil.toGrpcServiceIdentifier(new AciServiceIdentifier(UUID.randomUUID())))
            .setDeviceId(Device.PRIMARY_ID))
        .build());

    verifyNoInteractions(accountsManager);
    verifyNoInteractions(keysManager);
  }

  @Test
  void getPreKeysIncorrectUnidentifiedAccessKey() {
    final Account targetAccount = mock(Account.class);

    final UUID uuid = UUID.randomUUID();
    final AciServiceIdentifier identifier = new AciServiceIdentifier(uuid);
    final byte[] unidentifiedAccessKey = TestRandomUtil.nextBytes(UnidentifiedAccessUtil.UNIDENTIFIED_ACCESS_KEY_LENGTH);

    when(targetAccount.getUnidentifiedAccessKey()).thenReturn(Optional.of(unidentifiedAccessKey));
    when(accountsManager.getByServiceIdentifierAsync(identifier))
        .thenReturn(CompletableFuture.completedFuture(Optional.of(targetAccount)));

    final GetPreKeysAnonymousResponse response = unauthenticatedServiceStub().getPreKeys(
        GetPreKeysAnonymousRequest.newBuilder()
            .setUnidentifiedAccessKey(UUIDUtil.toByteString(UUID.randomUUID()))
            .setRequest(GetPreKeysRequest.newBuilder()
                .setTargetIdentifier(ServiceIdentifierUtil.toGrpcServiceIdentifier(identifier))
                .setDeviceId(Device.PRIMARY_ID))
            .build());

    assertTrue(response.hasFailedUnidentifiedAuthorization());
    verifyNoInteractions(keysManager);
  }

  @Test
  void getPreKeysExpiredGroupSendEndorsement() throws Exception {
    final UUID uuid = UUID.randomUUID();
    final AciServiceIdentifier identifier = new AciServiceIdentifier(uuid);

    // Expirations must be on day boundaries or libzonarosa will refuse to create or verify the token
    final Instant expiration = Instant.now().truncatedTo(ChronoUnit.DAYS);
    CLOCK.pin(expiration.plus(Duration.ofHours(1))); // set time so our token is already expired

    final byte[] token = AuthHelper.validGroupSendToken(SERVER_SECRET_PARAMS, List.of(identifier), expiration);

    final GetPreKeysAnonymousResponse preKeysResponse =
        unauthenticatedServiceStub().getPreKeys(GetPreKeysAnonymousRequest.newBuilder()
            .setGroupSendToken(ByteString.copyFrom(token))
            .setRequest(GetPreKeysRequest.newBuilder()
                .setTargetIdentifier(ServiceIdentifierUtil.toGrpcServiceIdentifier(identifier))
                .setDeviceId(Device.PRIMARY_ID))
            .build());
    assertTrue(preKeysResponse.hasFailedUnidentifiedAuthorization());

    verifyNoInteractions(accountsManager);
    verifyNoInteractions(keysManager);
  }

  @Test
  void getPreKeysIncorrectGroupSendEndorsement() throws Exception {
    final AciServiceIdentifier authorizedIdentifier = new AciServiceIdentifier(UUID.randomUUID());
    final AciServiceIdentifier targetIdentifier = new AciServiceIdentifier(UUID.randomUUID());

    // Expirations must be on day boundaries or libzonarosa will refuse to create or verify the token
    final Instant expiration = Instant.now().truncatedTo(ChronoUnit.DAYS);
    CLOCK.pin(expiration.minus(Duration.ofHours(1))); // set time so the credential isn't expired yet

    final AciServiceIdentifier wrongAci = new AciServiceIdentifier(UUID.randomUUID());
    final byte[] token = AuthHelper.validGroupSendToken(SERVER_SECRET_PARAMS, List.of(authorizedIdentifier), expiration);

    final GetPreKeysAnonymousResponse response = unauthenticatedServiceStub().getPreKeys(
        GetPreKeysAnonymousRequest.newBuilder()
            .setGroupSendToken(ByteString.copyFrom(token))
            .setRequest(GetPreKeysRequest.newBuilder()
                .setTargetIdentifier(ServiceIdentifierUtil.toGrpcServiceIdentifier(targetIdentifier))
                .setDeviceId(Device.PRIMARY_ID))
            .build());
    assertTrue(response.hasFailedUnidentifiedAuthorization());
    verifyNoInteractions(accountsManager);
    verifyNoInteractions(keysManager);
  }

  @Test
  void getPreKeysAccountNotFoundUnidentifiedAccessKey() {
    final AciServiceIdentifier nonexistentAci = new AciServiceIdentifier(UUID.randomUUID());
    when(accountsManager.getByServiceIdentifierAsync(nonexistentAci))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    final GetPreKeysAnonymousResponse preKeysResponse =
        unauthenticatedServiceStub().getPreKeys(GetPreKeysAnonymousRequest.newBuilder()
            .setUnidentifiedAccessKey(UUIDUtil.toByteString(UUID.randomUUID()))
            .setRequest(GetPreKeysRequest.newBuilder()
                .setTargetIdentifier(ServiceIdentifierUtil.toGrpcServiceIdentifier(nonexistentAci)))
            .build());
    assertTrue(preKeysResponse.hasFailedUnidentifiedAuthorization());
    verifyNoInteractions(keysManager);
  }

  @Test
  void getPreKeysAccountNotFoundGroupSendEndorsement() throws Exception {
    final AciServiceIdentifier nonexistentAci = new AciServiceIdentifier(UUID.randomUUID());

    // Expirations must be on day boundaries or libzonarosa will refuse to create or verify the token
    final Instant expiration = Instant.now().truncatedTo(ChronoUnit.DAYS);
    CLOCK.pin(expiration.minus(Duration.ofHours(1))); // set time so the credential isn't expired yet

    final byte[] token = AuthHelper.validGroupSendToken(SERVER_SECRET_PARAMS, List.of(nonexistentAci), expiration);

    when(accountsManager.getByServiceIdentifierAsync(nonexistentAci))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    final GetPreKeysAnonymousResponse preKeysResponse =
        unauthenticatedServiceStub().getPreKeys(GetPreKeysAnonymousRequest.newBuilder()
            .setGroupSendToken(ByteString.copyFrom(token))
            .setRequest(GetPreKeysRequest.newBuilder()
                .setTargetIdentifier(ServiceIdentifierUtil.toGrpcServiceIdentifier(nonexistentAci)))
        .build());
    assertTrue(preKeysResponse.hasTargetNotFound());
    verifyNoInteractions(keysManager);
  }

  @Test
  void getPreKeysDeviceNotFound() {
    final UUID accountIdentifier = UUID.randomUUID();

    final byte[] unidentifiedAccessKey = TestRandomUtil.nextBytes(UnidentifiedAccessUtil.UNIDENTIFIED_ACCESS_KEY_LENGTH);

    final Account targetAccount = mock(Account.class);
    when(targetAccount.getUuid()).thenReturn(accountIdentifier);
    when(targetAccount.getIdentityKey(IdentityType.ACI)).thenReturn(new IdentityKey(ECKeyPair.generate().getPublicKey()));
    when(targetAccount.getDevices()).thenReturn(Collections.emptyList());
    when(targetAccount.getDevice(anyByte())).thenReturn(Optional.empty());
    when(targetAccount.getUnidentifiedAccessKey()).thenReturn(Optional.of(unidentifiedAccessKey));

    when(accountsManager.getByServiceIdentifierAsync(new AciServiceIdentifier(accountIdentifier)))
        .thenReturn(CompletableFuture.completedFuture(Optional.of(targetAccount)));

    final GetPreKeysAnonymousResponse response = unauthenticatedServiceStub().getPreKeys(
        GetPreKeysAnonymousRequest.newBuilder()
            .setUnidentifiedAccessKey(ByteString.copyFrom(unidentifiedAccessKey))
            .setRequest(GetPreKeysRequest.newBuilder()
                .setTargetIdentifier(ServiceIdentifier.newBuilder()
                    .setIdentityType(io.zonarosa.chat.common.IdentityType.IDENTITY_TYPE_ACI)
                    .setUuid(UUIDUtil.toByteString(accountIdentifier)))
                .setDeviceId(Device.PRIMARY_ID))
            .build());

    assertTrue(response.hasFailedUnidentifiedAuthorization());
  }

  @Test
  void checkIdentityKeys() {
    final ReactorKeysAnonymousGrpc.ReactorKeysAnonymousStub reactiveKeysAnonymousStub = ReactorKeysAnonymousGrpc.newReactorStub(SimpleBaseGrpcTest.GRPC_SERVER_EXTENSION_UNAUTHENTICATED.getChannel());
    when(accountsManager.getByServiceIdentifierAsync(any()))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    final Account mismatchedAciFingerprintAccount = mock(Account.class);
    final UUID mismatchedAciFingerprintAccountIdentifier = UUID.randomUUID();
    final IdentityKey mismatchedAciFingerprintAccountIdentityKey = new IdentityKey(ECKeyPair.generate().getPublicKey());

    final Account matchingAciFingerprintAccount = mock(Account.class);
    final UUID matchingAciFingerprintAccountIdentifier = UUID.randomUUID();
    final IdentityKey matchingAciFingerprintAccountIdentityKey = new IdentityKey(ECKeyPair.generate().getPublicKey());

    final Account mismatchedPniFingerprintAccount = mock(Account.class);
    final UUID mismatchedPniFingerprintAccountIdentifier = UUID.randomUUID();
    final IdentityKey mismatchedPniFingerpringAccountIdentityKey = new IdentityKey(ECKeyPair.generate().getPublicKey());

    when(mismatchedAciFingerprintAccount.getIdentityKey(IdentityType.ACI)).thenReturn(mismatchedAciFingerprintAccountIdentityKey);
    when(accountsManager.getByServiceIdentifierAsync(new AciServiceIdentifier(mismatchedAciFingerprintAccountIdentifier)))
        .thenReturn(CompletableFuture.completedFuture(Optional.of(mismatchedAciFingerprintAccount)));

    when(matchingAciFingerprintAccount.getIdentityKey(IdentityType.ACI)).thenReturn(matchingAciFingerprintAccountIdentityKey);
    when(accountsManager.getByServiceIdentifierAsync(new AciServiceIdentifier(matchingAciFingerprintAccountIdentifier)))
        .thenReturn(CompletableFuture.completedFuture(Optional.of(matchingAciFingerprintAccount)));

    when(mismatchedPniFingerprintAccount.getIdentityKey(IdentityType.PNI)).thenReturn(mismatchedPniFingerpringAccountIdentityKey);
    when(accountsManager.getByServiceIdentifierAsync(new PniServiceIdentifier(mismatchedPniFingerprintAccountIdentifier)))
        .thenReturn(CompletableFuture.completedFuture(Optional.of(mismatchedPniFingerprintAccount)));

    final Flux<CheckIdentityKeyRequest> requests = Flux.just(
        buildCheckIdentityKeyRequest(io.zonarosa.chat.common.IdentityType.IDENTITY_TYPE_ACI, mismatchedAciFingerprintAccountIdentifier,
            new IdentityKey(ECKeyPair.generate().getPublicKey())),
        buildCheckIdentityKeyRequest(io.zonarosa.chat.common.IdentityType.IDENTITY_TYPE_ACI, matchingAciFingerprintAccountIdentifier,
            matchingAciFingerprintAccountIdentityKey),
        buildCheckIdentityKeyRequest(io.zonarosa.chat.common.IdentityType.IDENTITY_TYPE_PNI, UUID.randomUUID(),
            new IdentityKey(ECKeyPair.generate().getPublicKey())),
        buildCheckIdentityKeyRequest(io.zonarosa.chat.common.IdentityType.IDENTITY_TYPE_PNI, mismatchedPniFingerprintAccountIdentifier,
            new IdentityKey(ECKeyPair.generate().getPublicKey()))
    );

    final Map<UUID, IdentityKey> expectedResponses = Map.of(
        mismatchedAciFingerprintAccountIdentifier, mismatchedAciFingerprintAccountIdentityKey,
        mismatchedPniFingerprintAccountIdentifier, mismatchedPniFingerpringAccountIdentityKey);

    final Map<UUID, IdentityKey> responses = reactiveKeysAnonymousStub.checkIdentityKeys(requests)
        .collectMap(response -> ServiceIdentifierUtil.fromGrpcServiceIdentifier(response.getTargetIdentifier()).uuid(),
            response -> {
              try {
                return new IdentityKey(response.getIdentityKey().toByteArray());
              } catch (InvalidKeyException e) {
                throw new RuntimeException(e);
              }
            })
        .block();

    assertEquals(expectedResponses, responses);
  }

  private static CheckIdentityKeyRequest buildCheckIdentityKeyRequest(final io.zonarosa.chat.common.IdentityType identityType,
      final UUID uuid, final IdentityKey identityKey) {
    return CheckIdentityKeyRequest.newBuilder()
        .setTargetIdentifier(ServiceIdentifier.newBuilder()
            .setIdentityType(identityType)
            .setUuid(ByteString.copyFrom(UUIDUtil.toBytes(uuid))))
        .setFingerprint(ByteString.copyFrom(getFingerprint(identityKey)))
        .build();
  }

  private static byte[] getFingerprint(final IdentityKey publicKey) {
    try {
      return Util.truncate(MessageDigest.getInstance("SHA-256").digest(publicKey.serialize()), 4);
    } catch (final NoSuchAlgorithmException e) {
      throw new AssertionError("All Java implementations must support SHA-256 MessageDigest algorithm", e);
    }
  }

  private void assertGetKeysFailure(Status code, GetPreKeysAnonymousRequest request) {
    assertStatusException(code, () -> unauthenticatedServiceStub().getPreKeys(request));
  }

  private static EcPreKey toGrpcEcPreKey(final ECPreKey preKey) {
    return EcPreKey.newBuilder()
        .setKeyId(preKey.keyId())
        .setPublicKey(ByteString.copyFrom(preKey.publicKey().serialize()))
        .build();
  }

  private static EcSignedPreKey toGrpcEcSignedPreKey(final ECSignedPreKey preKey) {
    return EcSignedPreKey.newBuilder()
        .setKeyId(preKey.keyId())
        .setPublicKey(ByteString.copyFrom(preKey.publicKey().serialize()))
        .setSignature(ByteString.copyFrom(preKey.signature()))
        .build();
  }

  private static KemSignedPreKey toGrpcKemSignedPreKey(final KEMSignedPreKey preKey) {
    return KemSignedPreKey.newBuilder()
        .setKeyId(preKey.keyId())
        .setPublicKey(ByteString.copyFrom(preKey.publicKey().serialize()))
        .setSignature(ByteString.copyFrom(preKey.signature()))
        .build();
  }

}
