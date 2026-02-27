/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.server.spam;

import jakarta.ws.rs.container.ContainerRequestContext;
import java.util.Optional;
import jakarta.ws.rs.core.Response;
import io.zonarosa.chat.messages.SendMessageResponse;
import io.zonarosa.chat.messages.SendMultiRecipientMessageResponse;
import io.zonarosa.server.identity.ServiceIdentifier;
import io.zonarosa.server.storage.Account;

public interface SpamChecker {

  /**
   * Determine if a message sent to an individual recipient via HTTP may be spam.
   *
   * @param messageType      the type of message to check
   * @param requestContext   the request context for a message send attempt
   * @param maybeSource      the sender of the message, could be empty if this as message sent with sealed sender
   * @param maybeDestination the destination of the message, could be empty if the destination does not exist or could
   *                         not be retrieved
   * @param destinationIdentifier the service identifier for the destination account
   * @return A {@link SpamCheckResult}
   */
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  SpamCheckResult<Response> checkForIndividualRecipientSpamHttp(
      final MessageType messageType,
      final ContainerRequestContext requestContext,
      final Optional<io.zonarosa.server.auth.AuthenticatedDevice> maybeSource,
      final Optional<Account> maybeDestination,
      final ServiceIdentifier destinationIdentifier);

  /**
   * Determine if a message sent to multiple recipients via HTTP may be spam.
   *
   * @param messageType      the type of message to check
   * @param requestContext   the request context for a message send attempt
   * @return A {@link SpamCheckResult}
   */
  SpamCheckResult<Response> checkForMultiRecipientSpamHttp(
      final MessageType messageType,
      final ContainerRequestContext requestContext);

  /**
   * Determine if a message sent to an individual recipient via gRPC may be spam.
   *
   * @param messageType      the type of message to check
   * @param maybeSource      the sender of the message, could be empty if this as message sent with sealed sender
   * @param maybeDestination the destination of the message, could be empty if the destination does not exist or could
   *                         not be retrieved
   * @param destinationIdentifier the service identifier for the destination account
   * @return A {@link SpamCheckResult}
   */
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  SpamCheckResult<GrpcResponse<SendMessageResponse>> checkForIndividualRecipientSpamGrpc(
      final MessageType messageType,
      final Optional<io.zonarosa.server.auth.grpc.AuthenticatedDevice> maybeSource,
      final Optional<Account> maybeDestination,
      final ServiceIdentifier destinationIdentifier);

  /**
   * Determine if a message sent to multiple recipients via gRPC may be spam.
   *
   * @param messageType the type of message to check
   *
   * @return A {@link SpamCheckResult}
   */
  SpamCheckResult<GrpcResponse<SendMultiRecipientMessageResponse>> checkForMultiRecipientSpamGrpc(final MessageType messageType);


  static SpamChecker noop() {
    return new SpamChecker() {

      @Override
      public SpamCheckResult<Response> checkForIndividualRecipientSpamHttp(final MessageType messageType,
          final ContainerRequestContext requestContext,
          final Optional<io.zonarosa.server.auth.AuthenticatedDevice> maybeSource,
          final Optional<Account> maybeDestination,
          final ServiceIdentifier destinationIdentifier) {

        return new SpamCheckResult<>(Optional.empty(), Optional.empty());
      }

      @Override
      public SpamCheckResult<Response> checkForMultiRecipientSpamHttp(final MessageType messageType,
          final ContainerRequestContext requestContext) {

        return new SpamCheckResult<>(Optional.empty(), Optional.empty());
      }

      @Override
      public SpamCheckResult<GrpcResponse<SendMessageResponse>> checkForIndividualRecipientSpamGrpc(final MessageType messageType,
          final Optional<io.zonarosa.server.auth.grpc.AuthenticatedDevice> maybeSource,
          final Optional<Account> maybeDestination,
          final ServiceIdentifier destinationIdentifier) {

        return new SpamCheckResult<>(Optional.empty(), Optional.empty());
      }

      @Override
      public SpamCheckResult<GrpcResponse<SendMultiRecipientMessageResponse>> checkForMultiRecipientSpamGrpc(
          final MessageType messageType) {

        return new SpamCheckResult<>(Optional.empty(), Optional.empty());
      }
    };
  }
}
