/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.auth.grpc;

import io.grpc.Context;
import javax.annotation.Nullable;
import io.zonarosa.server.grpc.GrpcExceptions;
import io.zonarosa.server.storage.Device;

/**
 * Provides utility methods for working with authentication in the context of gRPC calls.
 */
public class AuthenticationUtil {

  static final Context.Key<AuthenticatedDevice> CONTEXT_AUTHENTICATED_DEVICE = Context.key("authenticated-device");

  /**
   * Returns the account/device authenticated in the current gRPC context. Should only be called from a service run with
   * the {@link RequireAuthenticationInterceptor}.
   *
   * @return the account/device identifier authenticated in the current gRPC context
   * @throws IllegalStateException if no authenticated account/device could be retrieved from the current gRPC context
   */
  public static AuthenticatedDevice requireAuthenticatedDevice() {
    @Nullable final AuthenticatedDevice authenticatedDevice = CONTEXT_AUTHENTICATED_DEVICE.get();

    if (authenticatedDevice != null) {
      return authenticatedDevice;
    }

    throw new IllegalStateException(
        "Configuration issue: service expects an authenticated device, but none was found. Request should have failed from an interceptor");
  }

  /**
   * Returns the account/device authenticated in the current gRPC context or "invalid argument" if the authenticated
   * device is not the primary device for the account.
   *
   * @return the account/device identifier authenticated in the current gRPC context
   * @throws io.grpc.StatusRuntimeException with a status of {@code INVALID_ARGUMENT} if the authenticated device is not
   *                                        the primary device for the authenticated account
   * @throws IllegalStateException          if no authenticated account/device could be retrieved from the current gRPC
   *                                        context
   */
  public static AuthenticatedDevice requireAuthenticatedPrimaryDevice() {
    final AuthenticatedDevice authenticatedDevice = requireAuthenticatedDevice();
    if (authenticatedDevice.deviceId() != Device.PRIMARY_ID) {
      throw GrpcExceptions.badAuthentication("RPC requires a primary device");
    }
    return authenticatedDevice;
  }
}
