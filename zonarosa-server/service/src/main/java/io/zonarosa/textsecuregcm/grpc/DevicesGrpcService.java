/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.grpc;

import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import io.zonarosa.chat.device.ClearPushTokenRequest;
import io.zonarosa.chat.device.ClearPushTokenResponse;
import io.zonarosa.chat.device.GetDevicesRequest;
import io.zonarosa.chat.device.GetDevicesResponse;
import io.zonarosa.chat.device.ReactorDevicesGrpc;
import io.zonarosa.chat.device.RemoveDeviceRequest;
import io.zonarosa.chat.device.RemoveDeviceResponse;
import io.zonarosa.chat.device.SetCapabilitiesRequest;
import io.zonarosa.chat.device.SetCapabilitiesResponse;
import io.zonarosa.chat.device.SetDeviceNameRequest;
import io.zonarosa.chat.device.SetDeviceNameResponse;
import io.zonarosa.chat.device.SetPushTokenRequest;
import io.zonarosa.chat.device.SetPushTokenResponse;
import io.zonarosa.chat.errors.NotFound;
import io.zonarosa.server.auth.grpc.AuthenticatedDevice;
import io.zonarosa.server.auth.grpc.AuthenticationUtil;
import io.zonarosa.server.identity.IdentityType;
import io.zonarosa.server.storage.Account;
import io.zonarosa.server.storage.AccountsManager;
import io.zonarosa.server.storage.Device;
import io.zonarosa.server.storage.DeviceCapability;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class DevicesGrpcService extends ReactorDevicesGrpc.DevicesImplBase {

  private final AccountsManager accountsManager;

  public DevicesGrpcService(final AccountsManager accountsManager) {
    this.accountsManager = accountsManager;
  }

  @Override
  public Mono<GetDevicesResponse> getDevices(final GetDevicesRequest request) {
    final AuthenticatedDevice authenticatedDevice = AuthenticationUtil.requireAuthenticatedDevice();

    return getAccount(authenticatedDevice.accountIdentifier())
        .flatMapMany(account -> Flux.fromIterable(account.getDevices()))
        .reduce(GetDevicesResponse.newBuilder(), (builder, device) -> {
          final GetDevicesResponse.LinkedDevice.Builder linkedDeviceBuilder = GetDevicesResponse.LinkedDevice.newBuilder();

          if (device.getName() != null) {
            linkedDeviceBuilder.setName(ByteString.copyFrom(device.getName()));
          }

          return builder.addDevices(linkedDeviceBuilder
              .setId(device.getId())
              .setCreated(device.getCreated())
              .setLastSeen(device.getLastSeen())
              .setRegistrationId(device.getRegistrationId(IdentityType.ACI))
              .setCreatedAtCiphertext(ByteString.copyFrom(device.getCreatedAtCiphertext()))
              .build());
        })
        .map(GetDevicesResponse.Builder::build);
  }

  @Override
  public Mono<RemoveDeviceResponse> removeDevice(final RemoveDeviceRequest request) {
    if (request.getId() == Device.PRIMARY_ID) {
      throw GrpcExceptions.invalidArguments("cannot remove primary device");
    }

    final AuthenticatedDevice authenticatedDevice = AuthenticationUtil.requireAuthenticatedDevice();

    if (authenticatedDevice.deviceId() != Device.PRIMARY_ID && request.getId() != authenticatedDevice.deviceId()) {
      throw GrpcExceptions.badAuthentication("linked devices cannot remove devices other than themselves");
    }

    final byte deviceId = DeviceIdUtil.validate(request.getId());

    return getAccount(authenticatedDevice.accountIdentifier())
        .flatMap(account -> Mono.fromFuture(accountsManager.removeDevice(account, deviceId)))
        .thenReturn(RemoveDeviceResponse.newBuilder().build());
  }

  @Override
  public Mono<SetDeviceNameResponse> setDeviceName(final SetDeviceNameRequest request) {
    final AuthenticatedDevice authenticatedDevice = AuthenticationUtil.requireAuthenticatedDevice();

    final byte deviceId = DeviceIdUtil.validate(request.getId());

    final boolean mayChangeName = authenticatedDevice.deviceId() == Device.PRIMARY_ID ||
        authenticatedDevice.deviceId() == deviceId;

    if (!mayChangeName) {
      throw GrpcExceptions.badAuthentication("linked device is not authorized to change target device name");
    }

    return getAccount(authenticatedDevice.accountIdentifier())
        .flatMap(account -> {
          if (account.getDevice(deviceId).isEmpty()) {
            return Mono.just(SetDeviceNameResponse.newBuilder().setTargetDeviceNotFound(NotFound.getDefaultInstance()).build());
          }
          return Mono.fromFuture(() -> accountsManager.updateDeviceAsync(account, deviceId, device ->
                      device.setName(request.getName().toByteArray())))
              .thenReturn(SetDeviceNameResponse.newBuilder().setSuccess(Empty.getDefaultInstance()).build());
        });
  }

  @Override
  public Mono<SetPushTokenResponse> setPushToken(final SetPushTokenRequest request) {
    final AuthenticatedDevice authenticatedDevice = AuthenticationUtil.requireAuthenticatedDevice();

    @Nullable final String apnsToken;
    @Nullable final String fcmToken;

    switch (request.getTokenRequestCase()) {

      case APNS_TOKEN_REQUEST -> {
        final SetPushTokenRequest.ApnsTokenRequest apnsTokenRequest = request.getApnsTokenRequest();
        apnsToken = StringUtils.stripToNull(apnsTokenRequest.getApnsToken());
        fcmToken = null;
      }

      case FCM_TOKEN_REQUEST -> {
        final SetPushTokenRequest.FcmTokenRequest fcmTokenRequest = request.getFcmTokenRequest();
        apnsToken = null;
        fcmToken = StringUtils.stripToNull(fcmTokenRequest.getFcmToken());
      }

      default -> throw GrpcExceptions.fieldViolation("token_request", "No tokens specified");
    }

    return getAccount(authenticatedDevice.accountIdentifier())
        .flatMap(account -> {
          final Device device = account.getDevice(authenticatedDevice.deviceId())
              .orElseThrow(() -> GrpcExceptions.invalidCredentials("invalid credentials"));

          final boolean tokenUnchanged =
              Objects.equals(device.getApnId(), apnsToken) &&
                  Objects.equals(device.getGcmId(), fcmToken);

          return tokenUnchanged
              ? Mono.empty()
              : Mono.fromFuture(() -> accountsManager.updateDeviceAsync(account, authenticatedDevice.deviceId(), d -> {
                d.setApnId(apnsToken);
                d.setGcmId(fcmToken);
                d.setFetchesMessages(false);
              }));
        })
        .thenReturn(SetPushTokenResponse.newBuilder().build());
  }

  @Override
  public Mono<ClearPushTokenResponse> clearPushToken(final ClearPushTokenRequest request) {
    final AuthenticatedDevice authenticatedDevice = AuthenticationUtil.requireAuthenticatedDevice();

    return getAccount(authenticatedDevice.accountIdentifier())
        .flatMap(account -> Mono.fromFuture(() -> accountsManager.updateDeviceAsync(account, authenticatedDevice.deviceId(), device -> {
          if (StringUtils.isNotBlank(device.getApnId())) {
            device.setUserAgent(device.isPrimary() ? "OWI" : "OWP");
          } else if (StringUtils.isNotBlank(device.getGcmId())) {
            device.setUserAgent("OWA");
          }

          device.setApnId(null);
          device.setGcmId(null);
          device.setFetchesMessages(true);
        })))
        .thenReturn(ClearPushTokenResponse.newBuilder().build());
  }

  @Override
  public Mono<SetCapabilitiesResponse> setCapabilities(final SetCapabilitiesRequest request) {
    final AuthenticatedDevice authenticatedDevice = AuthenticationUtil.requireAuthenticatedDevice();

    final Set<DeviceCapability> capabilities = request.getCapabilitiesList().stream()
        .map(DeviceCapabilityUtil::fromGrpcDeviceCapability)
        .collect(Collectors.toSet());

    return getAccount(authenticatedDevice.accountIdentifier())
        .flatMap(account ->
            Mono.fromFuture(() -> accountsManager.updateDeviceAsync(account, authenticatedDevice.deviceId(),
                d -> d.setCapabilities(capabilities))))
        .thenReturn(SetCapabilitiesResponse.newBuilder().build());
  }

  private Mono<Account> getAccount(final UUID accountIdentifier) {
    return Mono.fromFuture(() -> accountsManager.getByAccountIdentifierAsync(accountIdentifier))
        .map(maybeAccount -> maybeAccount.orElseThrow(() -> GrpcExceptions.invalidCredentials("invalid credentials")));
  }
}
