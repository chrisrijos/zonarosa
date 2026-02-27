/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.grpc;

import io.grpc.StatusRuntimeException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import io.zonarosa.chat.common.EcPreKey;
import io.zonarosa.chat.common.EcSignedPreKey;
import io.zonarosa.chat.common.KemSignedPreKey;
import io.zonarosa.chat.errors.NotFound;
import io.zonarosa.chat.keys.GetPreKeyCountRequest;
import io.zonarosa.chat.keys.GetPreKeyCountResponse;
import io.zonarosa.chat.keys.GetPreKeysRequest;
import io.zonarosa.chat.keys.GetPreKeysResponse;
import io.zonarosa.chat.keys.ReactorKeysGrpc;
import io.zonarosa.chat.keys.SetEcSignedPreKeyRequest;
import io.zonarosa.chat.keys.SetKemLastResortPreKeyRequest;
import io.zonarosa.chat.keys.SetOneTimeEcPreKeysRequest;
import io.zonarosa.chat.keys.SetOneTimeKemSignedPreKeysRequest;
import io.zonarosa.chat.keys.SetPreKeyResponse;
import io.zonarosa.libzonarosa.protocol.IdentityKey;
import io.zonarosa.libzonarosa.protocol.InvalidKeyException;
import io.zonarosa.libzonarosa.protocol.ecc.ECPublicKey;
import io.zonarosa.libzonarosa.protocol.kem.KEMPublicKey;
import io.zonarosa.server.auth.grpc.AuthenticatedDevice;
import io.zonarosa.server.auth.grpc.AuthenticationUtil;
import io.zonarosa.server.entities.ECPreKey;
import io.zonarosa.server.entities.ECSignedPreKey;
import io.zonarosa.server.entities.KEMSignedPreKey;
import io.zonarosa.server.identity.IdentityType;
import io.zonarosa.server.identity.ServiceIdentifier;
import io.zonarosa.server.limits.RateLimiters;
import io.zonarosa.server.storage.Account;
import io.zonarosa.server.storage.AccountsManager;
import io.zonarosa.server.storage.KeysManager;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

public class KeysGrpcService extends ReactorKeysGrpc.KeysImplBase {

  private final AccountsManager accountsManager;
  private final KeysManager keysManager;
  private final RateLimiters rateLimiters;

  private static final StatusRuntimeException INVALID_PUBLIC_KEY_EXCEPTION =
      GrpcExceptions.fieldViolation("pre_keys", "invalid public key");

  private static final StatusRuntimeException INVALID_SIGNATURE_EXCEPTION =
      GrpcExceptions.fieldViolation("pre_keys", "pre-key signature did not match account identity key");

  private enum PreKeyType {
    EC,
    KEM
  }

  public KeysGrpcService(final AccountsManager accountsManager,
      final KeysManager keysManager,
      final RateLimiters rateLimiters) {

    this.accountsManager = accountsManager;
    this.keysManager = keysManager;
    this.rateLimiters = rateLimiters;
  }

  @Override
  public Mono<GetPreKeyCountResponse> getPreKeyCount(final GetPreKeyCountRequest request) {
    return Mono.fromSupplier(AuthenticationUtil::requireAuthenticatedDevice)
        .flatMap(authenticatedDevice -> getAuthenticatedAccount(authenticatedDevice.accountIdentifier())
            .zipWith(Mono.just(authenticatedDevice.deviceId())))
        .flatMapMany(accountAndDeviceId -> Flux.just(
            Tuples.of(IdentityType.ACI, accountAndDeviceId.getT1().getUuid(), accountAndDeviceId.getT2()),
            Tuples.of(IdentityType.PNI, accountAndDeviceId.getT1().getPhoneNumberIdentifier(), accountAndDeviceId.getT2())
        ))
        .flatMap(identityTypeUuidAndDeviceId -> Flux.merge(
            Mono.fromFuture(() -> keysManager.getEcCount(identityTypeUuidAndDeviceId.getT2(), identityTypeUuidAndDeviceId.getT3()))
                .map(ecKeyCount -> Tuples.of(identityTypeUuidAndDeviceId.getT1(), PreKeyType.EC, ecKeyCount)),

            Mono.fromFuture(() -> keysManager.getPqCount(identityTypeUuidAndDeviceId.getT2(), identityTypeUuidAndDeviceId.getT3()))
                .map(ecKeyCount -> Tuples.of(identityTypeUuidAndDeviceId.getT1(), PreKeyType.KEM, ecKeyCount))
        ))
        .reduce(GetPreKeyCountResponse.newBuilder(), (builder, tuple) -> {
          final IdentityType identityType = tuple.getT1();
          final PreKeyType preKeyType = tuple.getT2();
          final int count = tuple.getT3();

          switch (identityType) {
            case ACI -> {
              switch (preKeyType) {
                case EC -> builder.setAciEcPreKeyCount(count);
                case KEM -> builder.setAciKemPreKeyCount(count);
              }
            }
            case PNI -> {
              switch (preKeyType) {
                case EC -> builder.setPniEcPreKeyCount(count);
                case KEM -> builder.setPniKemPreKeyCount(count);
              }
            }
          }

          return builder;
        })
        .map(GetPreKeyCountResponse.Builder::build);
  }

  @Override
  public Mono<GetPreKeysResponse> getPreKeys(final GetPreKeysRequest request) {
    final AuthenticatedDevice authenticatedDevice = AuthenticationUtil.requireAuthenticatedDevice();

    final ServiceIdentifier targetIdentifier =
        ServiceIdentifierUtil.fromGrpcServiceIdentifier(request.getTargetIdentifier());

    final byte deviceId = request.hasDeviceId()
        ? DeviceIdUtil.validate(request.getDeviceId())
        : KeysGrpcHelper.ALL_DEVICES;

    final String rateLimitKey = authenticatedDevice.accountIdentifier() + "." +
        authenticatedDevice.deviceId() + "__" +
        targetIdentifier.uuid() + "." +
        deviceId;

    return rateLimiters.getPreKeysLimiter().validateReactive(rateLimitKey)
        .then(Mono.fromFuture(() -> accountsManager.getByServiceIdentifierAsync(targetIdentifier)))
        .flatMap(Mono::justOrEmpty)
        .flatMap(targetAccount -> KeysGrpcHelper.getPreKeys(targetAccount, targetIdentifier, deviceId, keysManager))
        .map(bundles -> GetPreKeysResponse.newBuilder()
            .setPreKeys(bundles)
            .build())
        .switchIfEmpty(Mono.fromSupplier(() -> GetPreKeysResponse.newBuilder()
            .setTargetNotFound(NotFound.getDefaultInstance())
            .build()));
  }

  @Override
  public Mono<SetPreKeyResponse> setOneTimeEcPreKeys(final SetOneTimeEcPreKeysRequest request) {
    if (request.getPreKeysList().isEmpty()) {
      throw GrpcExceptions.fieldViolation("pre_keys", "pre_keys must be non-empty");
    }
    return Mono.fromSupplier(AuthenticationUtil::requireAuthenticatedDevice)
        .flatMap(authenticatedDevice -> storeOneTimePreKeys(authenticatedDevice.accountIdentifier(),
            request.getPreKeysList(),
            IdentityTypeUtil.fromGrpcIdentityType(request.getIdentityType()),
            (requestPreKey, ignored) -> checkEcPreKey(requestPreKey),
            (identifier, preKeys) -> keysManager.storeEcOneTimePreKeys(identifier, authenticatedDevice.deviceId(), preKeys)));
  }

  @Override
  public Mono<SetPreKeyResponse> setOneTimeKemSignedPreKeys(final SetOneTimeKemSignedPreKeysRequest request) {
    if (request.getPreKeysList().isEmpty()) {
      throw GrpcExceptions.fieldViolation("pre_keys", "pre_keys must be non-empty");
    }
    return Mono.fromSupplier(AuthenticationUtil::requireAuthenticatedDevice)
        .flatMap(authenticatedDevice -> storeOneTimePreKeys(authenticatedDevice.accountIdentifier(),
            request.getPreKeysList(),
            IdentityTypeUtil.fromGrpcIdentityType(request.getIdentityType()),
            KeysGrpcService::checkKemSignedPreKey,
            (identifier, preKeys) -> keysManager.storeKemOneTimePreKeys(identifier, authenticatedDevice.deviceId(), preKeys)));
  }

  private <K, R> Mono<SetPreKeyResponse> storeOneTimePreKeys(final UUID authenticatedAccountUuid,
      final List<R> requestPreKeys,
      final IdentityType identityType,
      final BiFunction<R, IdentityKey, K> extractPreKeyFunction,
      final BiFunction<UUID, List<K>, CompletableFuture<Void>> storeKeysFunction) {

    return getAuthenticatedAccount(authenticatedAccountUuid)
        .map(account -> {
          final List<K> preKeys = requestPreKeys.stream()
              .map(requestPreKey -> extractPreKeyFunction.apply(requestPreKey, account.getIdentityKey(identityType)))
              .toList();

          return Tuples.of(account.getIdentifier(identityType), preKeys);
        })
        .flatMap(identifierAndPreKeys -> Mono.fromFuture(() -> storeKeysFunction.apply(identifierAndPreKeys.getT1(), identifierAndPreKeys.getT2())))
        .thenReturn(SetPreKeyResponse.newBuilder().build());
  }

  @Override
  public Mono<SetPreKeyResponse> setEcSignedPreKey(final SetEcSignedPreKeyRequest request) {
    return Mono.fromSupplier(AuthenticationUtil::requireAuthenticatedDevice)
        .flatMap(authenticatedDevice -> storeRepeatedUseKey(authenticatedDevice.accountIdentifier(),
            request.getIdentityType(),
            request.getSignedPreKey(),
            KeysGrpcService::checkEcSignedPreKey,
            (account, signedPreKey) -> {
              final IdentityType identityType = IdentityTypeUtil.fromGrpcIdentityType(request.getIdentityType());
              final UUID identifier = account.getIdentifier(identityType);

              return Mono.fromFuture(() -> keysManager.storeEcSignedPreKeys(identifier, authenticatedDevice.deviceId(), signedPreKey));
            }));
  }

  @Override
  public Mono<SetPreKeyResponse> setKemLastResortPreKey(final SetKemLastResortPreKeyRequest request) {
    return Mono.fromSupplier(AuthenticationUtil::requireAuthenticatedDevice)
        .flatMap(authenticatedDevice -> storeRepeatedUseKey(authenticatedDevice.accountIdentifier(),
            request.getIdentityType(),
            request.getSignedPreKey(),
            KeysGrpcService::checkKemSignedPreKey,
            (account, lastResortKey) -> {
              final UUID identifier =
                  account.getIdentifier(IdentityTypeUtil.fromGrpcIdentityType(request.getIdentityType()));

              return Mono.fromFuture(() -> keysManager.storePqLastResort(identifier, authenticatedDevice.deviceId(), lastResortKey));
            }));
  }

  private <K, R> Mono<SetPreKeyResponse> storeRepeatedUseKey(final UUID authenticatedAccountUuid,
      final io.zonarosa.chat.common.IdentityType identityType,
      final R storeKeyRequest,
      final BiFunction<R, IdentityKey, K> extractKeyFunction,
      final BiFunction<Account, K, Mono<?>> storeKeyFunction) {

    return getAuthenticatedAccount(authenticatedAccountUuid)
        .map(account -> {
          final IdentityKey identityKey = account.getIdentityKey(IdentityTypeUtil.fromGrpcIdentityType(identityType));
          final K key = extractKeyFunction.apply(storeKeyRequest, identityKey);

          return Tuples.of(account, key);
        })
        .flatMap(accountAndKey -> storeKeyFunction.apply(accountAndKey.getT1(), accountAndKey.getT2()))
        .thenReturn(SetPreKeyResponse.newBuilder().build());
  }

  private static ECPreKey checkEcPreKey(final EcPreKey preKey) {
    try {
      return new ECPreKey(preKey.getKeyId(), new ECPublicKey(preKey.getPublicKey().toByteArray()));
    } catch (final InvalidKeyException e) {
      throw INVALID_PUBLIC_KEY_EXCEPTION;
    }
  }

  private static ECSignedPreKey checkEcSignedPreKey(final EcSignedPreKey preKey, final IdentityKey identityKey) {
    try {
      final ECSignedPreKey ecSignedPreKey = new ECSignedPreKey(preKey.getKeyId(),
          new ECPublicKey(preKey.getPublicKey().toByteArray()),
          preKey.getSignature().toByteArray());

      if (ecSignedPreKey.signatureValid(identityKey)) {
        return ecSignedPreKey;
      } else {
        throw INVALID_SIGNATURE_EXCEPTION;
      }
    } catch (final InvalidKeyException e) {
      throw INVALID_PUBLIC_KEY_EXCEPTION;
    }
  }

  private static KEMSignedPreKey checkKemSignedPreKey(final KemSignedPreKey preKey, final IdentityKey identityKey) {
    try {
      final KEMSignedPreKey kemSignedPreKey = new KEMSignedPreKey(preKey.getKeyId(),
          new KEMPublicKey(preKey.getPublicKey().toByteArray()),
          preKey.getSignature().toByteArray());

      if (kemSignedPreKey.signatureValid(identityKey)) {
        return kemSignedPreKey;
      } else {
        throw INVALID_SIGNATURE_EXCEPTION;
      }
    } catch (final InvalidKeyException e) {
      throw INVALID_PUBLIC_KEY_EXCEPTION;
    }
  }

  private Mono<Account> getAuthenticatedAccount(final UUID authenticatedAccountId) {
    return Mono.fromFuture(() -> accountsManager.getByAccountIdentifierAsync(authenticatedAccountId))
        .map(maybeAccount -> maybeAccount.orElseThrow(() -> GrpcExceptions.invalidCredentials("invalid credentials")));
  }
}
