/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.grpc;

import com.google.protobuf.ByteString;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.Arrays;
import io.zonarosa.chat.errors.FailedUnidentifiedAuthorization;
import io.zonarosa.chat.errors.NotFound;
import io.zonarosa.chat.keys.CheckIdentityKeyRequest;
import io.zonarosa.chat.keys.CheckIdentityKeyResponse;
import io.zonarosa.chat.keys.GetPreKeysAnonymousRequest;
import io.zonarosa.chat.keys.GetPreKeysAnonymousResponse;
import io.zonarosa.chat.keys.ReactorKeysAnonymousGrpc;
import io.zonarosa.libzonarosa.protocol.IdentityKey;
import io.zonarosa.libzonarosa.zkgroup.ServerSecretParams;
import io.zonarosa.server.auth.UnidentifiedAccessUtil;
import io.zonarosa.server.identity.ServiceIdentifier;
import io.zonarosa.server.storage.Account;
import io.zonarosa.server.storage.AccountsManager;
import io.zonarosa.server.storage.KeysManager;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

public class KeysAnonymousGrpcService extends ReactorKeysAnonymousGrpc.KeysAnonymousImplBase {

  private final AccountsManager accountsManager;
  private final KeysManager keysManager;
  private final GroupSendTokenUtil groupSendTokenUtil;

  public KeysAnonymousGrpcService(
      final AccountsManager accountsManager, final KeysManager keysManager, final ServerSecretParams serverSecretParams, final Clock clock) {
    this.accountsManager = accountsManager;
    this.keysManager = keysManager;
    groupSendTokenUtil = new GroupSendTokenUtil(serverSecretParams, clock);
  }

  @Override
  public Mono<GetPreKeysAnonymousResponse> getPreKeys(final GetPreKeysAnonymousRequest request) {
    final ServiceIdentifier serviceIdentifier =
        ServiceIdentifierUtil.fromGrpcServiceIdentifier(request.getRequest().getTargetIdentifier());

    final byte deviceId = request.getRequest().hasDeviceId()
        ? DeviceIdUtil.validate(request.getRequest().getDeviceId())
        : KeysGrpcHelper.ALL_DEVICES;

    return switch (request.getAuthorizationCase()) {
      case GROUP_SEND_TOKEN -> {
        if (!groupSendTokenUtil.checkGroupSendToken(request.getGroupSendToken(), serviceIdentifier)) {
          yield Mono.fromSupplier(() -> GetPreKeysAnonymousResponse.newBuilder()
              .setFailedUnidentifiedAuthorization(FailedUnidentifiedAuthorization.getDefaultInstance())
              .build());
        }
        yield lookUpAccount(serviceIdentifier)
            .flatMap(targetAccount -> KeysGrpcHelper
                .getPreKeys(targetAccount, serviceIdentifier, deviceId, keysManager))
            .map(preKeys -> GetPreKeysAnonymousResponse.newBuilder().setPreKeys(preKeys).build())
            .switchIfEmpty(Mono.fromSupplier(() -> GetPreKeysAnonymousResponse.newBuilder()
                .setTargetNotFound(NotFound.getDefaultInstance())
                .build()));
      }
      case UNIDENTIFIED_ACCESS_KEY -> lookUpAccount(serviceIdentifier)
          .filter(targetAccount ->
              UnidentifiedAccessUtil.checkUnidentifiedAccess(targetAccount, request.getUnidentifiedAccessKey().toByteArray()))
          .flatMap(targetAccount -> KeysGrpcHelper.getPreKeys(targetAccount, serviceIdentifier, deviceId, keysManager))
          .map(preKeys -> GetPreKeysAnonymousResponse.newBuilder().setPreKeys(preKeys).build())
          .switchIfEmpty(Mono.fromSupplier(() -> GetPreKeysAnonymousResponse.newBuilder()
              .setFailedUnidentifiedAuthorization(FailedUnidentifiedAuthorization.getDefaultInstance())
              .build()));

      default -> Mono.error(GrpcExceptions.fieldViolation("authorization", "invalid authorization type"));
    };
  }

  @Override
  public Flux<CheckIdentityKeyResponse> checkIdentityKeys(final Flux<CheckIdentityKeyRequest> requests) {
    return requests
        .map(request -> Tuples.of(ServiceIdentifierUtil.fromGrpcServiceIdentifier(request.getTargetIdentifier()),
            request.getFingerprint().toByteArray()))
        .flatMap(serviceIdentifierAndFingerprint -> Mono.fromFuture(
                () -> accountsManager.getByServiceIdentifierAsync(serviceIdentifierAndFingerprint.getT1()))
            .flatMap(Mono::justOrEmpty)
            .filter(account -> !fingerprintMatches(account.getIdentityKey(serviceIdentifierAndFingerprint.getT1()
                .identityType()), serviceIdentifierAndFingerprint.getT2()))
            .map(account -> CheckIdentityKeyResponse.newBuilder()
                    .setTargetIdentifier(
                        ServiceIdentifierUtil.toGrpcServiceIdentifier(serviceIdentifierAndFingerprint.getT1()))
                    .setIdentityKey(ByteString.copyFrom(account.getIdentityKey(serviceIdentifierAndFingerprint.getT1()
                        .identityType()).serialize()))
                    .build())
        );
  }

  private Mono<Account> lookUpAccount(final ServiceIdentifier serviceIdentifier) {
    return Mono.fromFuture(() -> accountsManager.getByServiceIdentifierAsync(serviceIdentifier))
        .flatMap(Mono::justOrEmpty);
  }

  private static boolean fingerprintMatches(final IdentityKey identityKey, final byte[] fingerprint) {
    final byte[] digest;
    try {
      digest = MessageDigest.getInstance("SHA-256").digest(identityKey.serialize());
    } catch (NoSuchAlgorithmException e) {
      // SHA-256 should always be supported as an algorithm
      throw new AssertionError("All Java implementations must support the SHA-256 message digest");
    }

    return Arrays.equals(digest, 0, 4, fingerprint, 0, 4);
  }
}
