/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.grpc;

import com.google.protobuf.ByteString;
import io.zonarosa.chat.common.EcPreKey;
import io.zonarosa.chat.common.EcSignedPreKey;
import io.zonarosa.chat.common.KemSignedPreKey;
import io.zonarosa.chat.keys.AccountPreKeyBundles;
import io.zonarosa.chat.keys.DevicePreKeyBundle;
import io.zonarosa.libzonarosa.protocol.IdentityKey;
import io.zonarosa.server.identity.ServiceIdentifier;
import io.zonarosa.server.storage.Account;
import io.zonarosa.server.storage.Device;
import io.zonarosa.server.storage.KeysManager;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import java.util.Optional;

class KeysGrpcHelper {

  static final byte ALL_DEVICES = 0;

  /**
   * Fetch {@link AccountPreKeyBundles} from the targetAccount
   *
   * @param targetAccount The targetAccount to fetch pre-key bundles from
   * @param targetServiceIdentifier The serviceIdentifier used to lookup the targetAccount
   * @param targetDeviceId The deviceId to retrieve pre-key bundles for, or ALL_DEVICES if all devices should be retrieved
   * @param keysManager The {@link KeysManager} to lookup pre-keys from
   * @return The requested bundles, or an empty Mono if the keys for the targetAccount do not exist
   */
  static Mono<AccountPreKeyBundles> getPreKeys(final Account targetAccount,
      final ServiceIdentifier targetServiceIdentifier,
      final byte targetDeviceId,
      final KeysManager keysManager) {

    final Flux<Device> devices = targetDeviceId == ALL_DEVICES
        ? Flux.fromIterable(targetAccount.getDevices())
        : Flux.from(Mono.justOrEmpty(targetAccount.getDevice(targetDeviceId)));

    final String userAgent = RequestAttributesUtil.getUserAgent().orElse(null);
    return devices
        .flatMap(device -> {
          final int registrationId = device.getRegistrationId(targetServiceIdentifier.identityType());
          return Mono
              .fromFuture(keysManager.takeDevicePreKeys(device.getId(), targetServiceIdentifier, userAgent))
              .flatMap(Mono::justOrEmpty)
              .map(devicePreKeys -> {
                final DevicePreKeyBundle.Builder builder = DevicePreKeyBundle.newBuilder()
                    .setEcSignedPreKey(EcSignedPreKey.newBuilder()
                        .setKeyId(devicePreKeys.ecSignedPreKey().keyId())
                        .setPublicKey(ByteString.copyFrom(devicePreKeys.ecSignedPreKey().serializedPublicKey()))
                        .setSignature(ByteString.copyFrom(devicePreKeys.ecSignedPreKey().signature()))
                        .build())
                    .setKemOneTimePreKey(KemSignedPreKey.newBuilder()
                        .setKeyId(devicePreKeys.kemSignedPreKey().keyId())
                        .setPublicKey(ByteString.copyFrom(devicePreKeys.kemSignedPreKey().serializedPublicKey()))
                        .setSignature(ByteString.copyFrom(devicePreKeys.kemSignedPreKey().signature()))
                        .build())
                    .setRegistrationId(registrationId);
                devicePreKeys.ecPreKey().ifPresent(ecPreKey -> builder.setEcOneTimePreKey(EcPreKey.newBuilder()
                    .setKeyId(ecPreKey.keyId())
                    .setPublicKey(ByteString.copyFrom(ecPreKey.serializedPublicKey()))
                    .build()));
                // Cast device IDs to `int` to match data types in the response object’s protobuf definition
                return Tuples.of((int) device.getId(), builder.build());
              });
        })
        .collectMap(Tuple2::getT1, Tuple2::getT2)
        .flatMap(preKeyBundles -> {
          if (preKeyBundles.isEmpty()) {
            // If there were no devices with valid prekey bundles in the account, the account is gone
            return Mono.empty();
          }

          final IdentityKey targetIdentityKey = targetAccount.getIdentityKey(targetServiceIdentifier.identityType());
          return Mono.just(AccountPreKeyBundles.newBuilder()
              .setIdentityKey(ByteString.copyFrom(targetIdentityKey.serialize()))
              .putAllDevicePreKeys(preKeyBundles)
              .build());
        });
  }
}
