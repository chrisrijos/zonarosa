/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.server.auth.grpc;

import io.dropwizard.auth.basic.BasicCredentials;
import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import java.util.Optional;
import io.zonarosa.server.auth.AccountAuthenticator;
import io.zonarosa.server.grpc.GrpcExceptions;
import io.zonarosa.server.grpc.ServerInterceptorUtil;
import io.zonarosa.server.util.HeaderUtils;

/**
 * A "require authentication" interceptor authenticates requests and attaches the {@link AuthenticatedDevice} to the
 * current gRPC context. Calls without authentication or with invalid credentials are closed with an
 * {@code UNAUTHENTICATED} status. If a call's authentication status cannot be determined (i.e. because the accounts
 * database is unavailable), the interceptor will reject the call with a status of {@code UNAVAILABLE}.
 */
public class RequireAuthenticationInterceptor implements ServerInterceptor {

  static final String AUTHORIZATION_HEADER = "authorization";

  private final AccountAuthenticator authenticator;

  public RequireAuthenticationInterceptor(final AccountAuthenticator authenticator) {
    this.authenticator = authenticator;
  }

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(final ServerCall<ReqT, RespT> call,
      final Metadata headers, final ServerCallHandler<ReqT, RespT> next) {
    final String authHeaderString = headers.get(
        Metadata.Key.of(AUTHORIZATION_HEADER, Metadata.ASCII_STRING_MARSHALLER));

    if (authHeaderString == null) {
      return ServerInterceptorUtil.closeWithStatusException(call,
          GrpcExceptions.invalidCredentials("missing authorization header"));
    }

    final Optional<BasicCredentials> basicCredentials = HeaderUtils.basicCredentialsFromAuthHeader(authHeaderString);
    if (basicCredentials.isEmpty()) {
      return ServerInterceptorUtil.closeWithStatusException(call,
          GrpcExceptions.invalidCredentials("malformed authorization header"));
    }

    final Optional<io.zonarosa.server.auth.AuthenticatedDevice> authenticated =
        authenticator.authenticate(basicCredentials.get());
    if (authenticated.isEmpty()) {
      return ServerInterceptorUtil.closeWithStatusException(call,
          GrpcExceptions.invalidCredentials("invalid credentials"));
    }

    final AuthenticatedDevice authenticatedDevice = new AuthenticatedDevice(
        authenticated.get().accountIdentifier(),
        authenticated.get().deviceId());

    return Contexts.interceptCall(Context.current()
            .withValue(AuthenticationUtil.CONTEXT_AUTHENTICATED_DEVICE, authenticatedDevice),
        call, headers, next);
  }
}
