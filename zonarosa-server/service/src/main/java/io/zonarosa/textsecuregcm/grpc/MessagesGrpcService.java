/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.grpc;

import com.google.protobuf.ByteString;
import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import io.zonarosa.chat.errors.NotFound;
import io.zonarosa.chat.messages.AuthenticatedSenderMessageType;
import io.zonarosa.chat.messages.IndividualRecipientMessageBundle;
import io.zonarosa.chat.messages.SendAuthenticatedSenderMessageRequest;
import io.zonarosa.chat.messages.SendMessageResponse;
import io.zonarosa.chat.messages.SendSyncMessageRequest;
import io.zonarosa.chat.messages.SimpleMessagesGrpc;
import io.zonarosa.server.auth.grpc.AuthenticatedDevice;
import io.zonarosa.server.auth.grpc.AuthenticationUtil;
import io.zonarosa.server.controllers.RateLimitExceededException;
import io.zonarosa.server.entities.MessageProtos;
import io.zonarosa.server.identity.AciServiceIdentifier;
import io.zonarosa.server.identity.ServiceIdentifier;
import io.zonarosa.server.limits.CardinalityEstimator;
import io.zonarosa.server.limits.RateLimiters;
import io.zonarosa.server.push.MessageSender;
import io.zonarosa.server.spam.GrpcResponse;
import io.zonarosa.server.spam.MessageType;
import io.zonarosa.server.spam.SpamCheckResult;
import io.zonarosa.server.spam.SpamChecker;
import io.zonarosa.server.storage.Account;
import io.zonarosa.server.storage.AccountsManager;

public class MessagesGrpcService extends SimpleMessagesGrpc.MessagesImplBase {

  private final AccountsManager accountsManager;
  private final RateLimiters rateLimiters;
  private final MessageSender messageSender;
  private final CardinalityEstimator messageByteLimitEstimator;
  private final SpamChecker spamChecker;
  private final Clock clock;

  public MessagesGrpcService(final AccountsManager accountsManager,
      final RateLimiters rateLimiters,
      final MessageSender messageSender,
      final CardinalityEstimator messageByteLimitEstimator,
      final SpamChecker spamChecker,
      final Clock clock) {

    this.accountsManager = accountsManager;
    this.rateLimiters = rateLimiters;
    this.messageSender = messageSender;
    this.messageByteLimitEstimator = messageByteLimitEstimator;
    this.spamChecker = spamChecker;
    this.clock = clock;
  }

  @Override
  public SendMessageResponse sendMessage(final SendAuthenticatedSenderMessageRequest request)
      throws RateLimitExceededException {

    final AuthenticatedDevice authenticatedDevice = AuthenticationUtil.requireAuthenticatedDevice();
    final AciServiceIdentifier senderServiceIdentifier = new AciServiceIdentifier(authenticatedDevice.accountIdentifier());
    final Account sender = accountsManager.getByServiceIdentifier(senderServiceIdentifier)
        .orElseThrow(() -> GrpcExceptions.invalidCredentials("invalid credentials"));

    final ServiceIdentifier destinationServiceIdentifier =
        ServiceIdentifierUtil.fromGrpcServiceIdentifier(request.getDestination());

    if (sender.isIdentifiedBy(destinationServiceIdentifier)) {
      throw GrpcExceptions.invalidArguments("use `sendSyncMessage` to send messages to own account");
    }

    final Optional<Account> maybeDestination = accountsManager.getByServiceIdentifier(destinationServiceIdentifier);
    if (maybeDestination.isEmpty()) {
      return SendMessageResponse.newBuilder().setDestinationNotFound(NotFound.getDefaultInstance()).build();
    }
    final Account destination = maybeDestination.get();

    rateLimiters.getMessagesLimiter().validate(authenticatedDevice.accountIdentifier(), destination.getUuid());

    return sendMessage(destination,
        destinationServiceIdentifier,
        authenticatedDevice,
        request.getType(),
        MessageType.INDIVIDUAL_IDENTIFIED_SENDER,
        request.getMessages(),
        request.getEphemeral(),
        request.getUrgent());
  }

  @Override
  public SendMessageResponse sendSyncMessage(final SendSyncMessageRequest request)
      throws RateLimitExceededException {

    final AuthenticatedDevice authenticatedDevice = AuthenticationUtil.requireAuthenticatedDevice();
    final AciServiceIdentifier senderServiceIdentifier = new AciServiceIdentifier(authenticatedDevice.accountIdentifier());
    final Account sender = accountsManager.getByServiceIdentifier(senderServiceIdentifier)
        .orElseThrow(() -> GrpcExceptions.invalidCredentials("invalid credentials"));

    return sendMessage(sender,
        senderServiceIdentifier,
        authenticatedDevice,
        request.getType(),
        MessageType.SYNC,
        request.getMessages(),
        false,
        request.getUrgent());
  }

  private SendMessageResponse sendMessage(final Account destination,
      final ServiceIdentifier destinationServiceIdentifier,
      final AuthenticatedDevice sender,
      final AuthenticatedSenderMessageType envelopeType,
      final MessageType messageType,
      final IndividualRecipientMessageBundle messages,
      final boolean ephemeral,
      final boolean urgent) throws RateLimitExceededException {

    try {
      final int totalPayloadLength = messages.getMessagesMap().values().stream()
          .mapToInt(message -> message.getPayload().size())
          .sum();

      rateLimiters.getInboundMessageBytes().validate(destinationServiceIdentifier.uuid(), totalPayloadLength);
    } catch (final RateLimitExceededException e) {
      messageByteLimitEstimator.add(destinationServiceIdentifier.uuid().toString());
      throw e;
    }

    final SpamCheckResult<GrpcResponse<SendMessageResponse>> spamCheckResult =
        spamChecker.checkForIndividualRecipientSpamGrpc(messageType,
            Optional.of(sender),
            Optional.of(destination),
            destinationServiceIdentifier);

    if (spamCheckResult.response().isPresent()) {
      return spamCheckResult.response().get().getResponseOrThrowStatus();
    }

    final Map<Byte, MessageProtos.Envelope> messagesByDeviceId = messages.getMessagesMap().entrySet()
        .stream()
        .collect(Collectors.toMap(
            entry -> DeviceIdUtil.validate(entry.getKey()),
            entry -> {
              final MessageProtos.Envelope.Builder envelopeBuilder = MessageProtos.Envelope.newBuilder()
                  .setType(getEnvelopeType(envelopeType))
                  .setClientTimestamp(messages.getTimestamp())
                  .setServerTimestamp(clock.millis())
                  .setDestinationServiceId(destinationServiceIdentifier.toServiceIdentifierString())
                  .setSourceServiceId(new AciServiceIdentifier(sender.accountIdentifier()).toServiceIdentifierString())
                  .setSourceDevice(sender.deviceId())
                  .setEphemeral(ephemeral)
                  .setUrgent(urgent)
                  .setContent(entry.getValue().getPayload());

              spamCheckResult.token().ifPresent(reportSpamToken ->
                  envelopeBuilder.setReportSpamToken(ByteString.copyFrom(reportSpamToken)));

              return envelopeBuilder.build();
            }
        ));

    final Map<Byte, Integer> registrationIdsByDeviceId = messages.getMessagesMap().entrySet().stream()
        .collect(Collectors.toMap(
            entry -> entry.getKey().byteValue(),
            entry -> entry.getValue().getRegistrationId()));

    return MessagesGrpcHelper.sendMessage(messageSender,
        destination,
        destinationServiceIdentifier,
        messagesByDeviceId,
        registrationIdsByDeviceId,
        messageType == MessageType.SYNC ? Optional.of(sender.deviceId()) : Optional.empty());
  }

  private static MessageProtos.Envelope.Type getEnvelopeType(final AuthenticatedSenderMessageType type) {
    return switch (type) {
      case DOUBLE_RATCHET -> MessageProtos.Envelope.Type.CIPHERTEXT;
      case PREKEY_MESSAGE -> MessageProtos.Envelope.Type.PREKEY_BUNDLE;
      case PLAINTEXT_CONTENT -> MessageProtos.Envelope.Type.PLAINTEXT_CONTENT;
      case UNSPECIFIED, UNRECOGNIZED ->
          throw GrpcExceptions.invalidArguments("unrecognized envelope type");
    };
  }
}
