/*
 * Copyright (C) 2014-2016 ZonaRosa Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */

package io.zonarosa.service.api.crypto;

import io.zonarosa.libzonarosa.metadata.InvalidMetadataMessageException;
import io.zonarosa.libzonarosa.metadata.InvalidMetadataVersionException;
import io.zonarosa.libzonarosa.metadata.ProtocolDuplicateMessageException;
import io.zonarosa.libzonarosa.metadata.ProtocolInvalidKeyException;
import io.zonarosa.libzonarosa.metadata.ProtocolInvalidKeyIdException;
import io.zonarosa.libzonarosa.metadata.ProtocolInvalidMessageException;
import io.zonarosa.libzonarosa.metadata.ProtocolInvalidVersionException;
import io.zonarosa.libzonarosa.metadata.ProtocolLegacyMessageException;
import io.zonarosa.libzonarosa.metadata.ProtocolNoSessionException;
import io.zonarosa.libzonarosa.metadata.ProtocolUntrustedIdentityException;
import io.zonarosa.libzonarosa.metadata.SealedSessionCipher;
import io.zonarosa.libzonarosa.metadata.SealedSessionCipher.DecryptionResult;
import io.zonarosa.libzonarosa.metadata.SelfSendException;
import io.zonarosa.libzonarosa.metadata.certificate.CertificateValidator;
import io.zonarosa.libzonarosa.metadata.certificate.SenderCertificate;
import io.zonarosa.libzonarosa.metadata.protocol.UnidentifiedSenderMessageContent;
import io.zonarosa.libzonarosa.protocol.DuplicateMessageException;
import io.zonarosa.libzonarosa.protocol.InvalidKeyException;
import io.zonarosa.libzonarosa.protocol.InvalidKeyIdException;
import io.zonarosa.libzonarosa.protocol.InvalidMessageException;
import io.zonarosa.libzonarosa.protocol.InvalidRegistrationIdException;
import io.zonarosa.libzonarosa.protocol.InvalidSessionException;
import io.zonarosa.libzonarosa.protocol.InvalidVersionException;
import io.zonarosa.libzonarosa.protocol.LegacyMessageException;
import io.zonarosa.libzonarosa.protocol.NoSessionException;
import io.zonarosa.libzonarosa.protocol.SessionCipher;
import io.zonarosa.libzonarosa.protocol.ZonaRosaProtocolAddress;
import io.zonarosa.libzonarosa.protocol.UntrustedIdentityException;
import io.zonarosa.libzonarosa.protocol.groups.GroupCipher;
import io.zonarosa.libzonarosa.protocol.logging.Log;
import io.zonarosa.libzonarosa.protocol.message.CiphertextMessage;
import io.zonarosa.libzonarosa.protocol.message.PlaintextContent;
import io.zonarosa.libzonarosa.protocol.message.PreKeyZonaRosaMessage;
import io.zonarosa.libzonarosa.protocol.message.ZonaRosaMessage;
import io.zonarosa.libzonarosa.protocol.state.SessionRecord;
import io.zonarosa.service.api.InvalidMessageStructureException;
import io.zonarosa.service.api.ZonaRosaServiceAccountDataStore;
import io.zonarosa.service.api.ZonaRosaSessionLock;
import io.zonarosa.service.api.messages.ZonaRosaServiceMetadata;
import io.zonarosa.service.api.push.DistributionId;
import io.zonarosa.core.models.ServiceId;
import io.zonarosa.core.models.ServiceId.ACI;
import io.zonarosa.service.api.push.ZonaRosaServiceAddress;
import io.zonarosa.core.util.UuidUtil;
import io.zonarosa.service.internal.push.Content;
import io.zonarosa.service.internal.push.Envelope;
import io.zonarosa.service.internal.push.OutgoingPushMessage;
import io.zonarosa.service.internal.push.PushTransportDetails;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

/**
 * This is used to encrypt + decrypt received envelopes.
 */
public class ZonaRosaServiceCipher {

  @SuppressWarnings("unused")
  private static final String TAG = ZonaRosaServiceCipher.class.getSimpleName();

  private final ZonaRosaServiceAccountDataStore zonarosaProtocolStore;
  private final ZonaRosaSessionLock             sessionLock;
  private final ZonaRosaServiceAddress localAddress;
  private final int                  localDeviceId;
  private final CertificateValidator certificateValidator;

  public ZonaRosaServiceCipher(ZonaRosaServiceAddress localAddress,
                             int localDeviceId,
                             ZonaRosaServiceAccountDataStore zonarosaProtocolStore,
                             ZonaRosaSessionLock sessionLock,
                             CertificateValidator certificateValidator)
  {
    this.zonarosaProtocolStore  = zonarosaProtocolStore;
    this.sessionLock          = sessionLock;
    this.localAddress         = localAddress;
    this.localDeviceId        = localDeviceId;
    this.certificateValidator = certificateValidator;
  }

  public byte[] encryptForGroup(DistributionId distributionId,
                                List<ZonaRosaProtocolAddress> destinations,
                                Map<ZonaRosaProtocolAddress, SessionRecord> sessionMap,
                                SenderCertificate senderCertificate,
                                byte[] unpaddedMessage,
                                ContentHint contentHint,
                                Optional<byte[]> groupId)
      throws NoSessionException, UntrustedIdentityException, InvalidKeyException, InvalidRegistrationIdException
  {
    PushTransportDetails             transport            = new PushTransportDetails();
    ZonaRosaProtocolAddress            localProtocolAddress = new ZonaRosaProtocolAddress(localAddress.getIdentifier(), localDeviceId);
    ZonaRosaGroupCipher                groupCipher          = new ZonaRosaGroupCipher(sessionLock, new GroupCipher(zonarosaProtocolStore, localProtocolAddress));
    ZonaRosaSealedSessionCipher        sessionCipher        = new ZonaRosaSealedSessionCipher(sessionLock, new SealedSessionCipher(zonarosaProtocolStore, localAddress.getServiceId().getRawUuid(), localAddress.getNumber().orElse(null), localDeviceId));
    CiphertextMessage                message              = groupCipher.encrypt(distributionId.asUuid(), transport.getPaddedMessageBody(unpaddedMessage));
    UnidentifiedSenderMessageContent messageContent       = new UnidentifiedSenderMessageContent(message,
                                                                                                 senderCertificate,
                                                                                                 contentHint.getType(),
                                                                                                 groupId);

    return sessionCipher.multiRecipientEncrypt(destinations, sessionMap, messageContent);
  }

  public OutgoingPushMessage encrypt(ZonaRosaProtocolAddress destination,
                                     @Nullable SealedSenderAccess sealedSenderAccess,
                                     EnvelopeContent content)
      throws UntrustedIdentityException, InvalidKeyException
  {
    try {
      ZonaRosaSessionCipher sessionCipher = new ZonaRosaSessionCipher(sessionLock, new SessionCipher(zonarosaProtocolStore, destination));
      if (sealedSenderAccess != null) {
        ZonaRosaSealedSessionCipher sealedSessionCipher = new ZonaRosaSealedSessionCipher(sessionLock, new SealedSessionCipher(zonarosaProtocolStore, localAddress.getServiceId().getRawUuid(), localAddress.getNumber()
                                                                                                                                                                                                      .orElse(null), localDeviceId));

        return content.processSealedSender(sessionCipher, sealedSessionCipher, destination, sealedSenderAccess.getSenderCertificate());
      } else {
        return content.processUnsealedSender(sessionCipher, destination);
      }
    } catch (NoSessionException e) {
      throw new InvalidSessionException("Session not found: " + destination);
    }
  }

  public ZonaRosaServiceCipherResult decrypt(Envelope envelope, long serverDeliveredTimestamp)
      throws InvalidMetadataMessageException, InvalidMetadataVersionException,
             ProtocolInvalidKeyIdException, ProtocolLegacyMessageException,
             ProtocolUntrustedIdentityException, ProtocolNoSessionException,
             ProtocolInvalidVersionException, ProtocolInvalidMessageException,
             ProtocolInvalidKeyException, ProtocolDuplicateMessageException,
             SelfSendException, InvalidMessageStructureException
  {
    try {
      if (envelope.content != null) {
        Plaintext plaintext = decryptInternal(envelope, serverDeliveredTimestamp);
        Content   content   = Content.ADAPTER.decode(plaintext.getData());

        return new ZonaRosaServiceCipherResult(
            content,
            new EnvelopeMetadata(
                plaintext.metadata.getSender().getServiceId(),
                plaintext.metadata.getSender().getNumber().orElse(null),
                plaintext.metadata.getSenderDevice(),
                plaintext.metadata.isNeedsReceipt(),
                plaintext.metadata.getGroupId().orElse(null),
                localAddress.getServiceId(),
                plaintext.getCiphertextMessageType()
            )
        );
      } else {
        return null;
      }
    } catch (IOException | IllegalArgumentException e) {
      throw new InvalidMetadataMessageException(e);
    }
  }

  private Plaintext decryptInternal(Envelope envelope, long serverDeliveredTimestamp)
      throws InvalidMetadataMessageException, InvalidMetadataVersionException,
      ProtocolDuplicateMessageException, ProtocolUntrustedIdentityException,
      ProtocolLegacyMessageException, ProtocolInvalidKeyException,
      ProtocolInvalidVersionException, ProtocolInvalidMessageException,
      ProtocolInvalidKeyIdException, ProtocolNoSessionException,
      SelfSendException, InvalidMessageStructureException
  {
    ServiceId sourceServiceId = ServiceId.parseOrNull(envelope.sourceServiceId, envelope.sourceServiceIdBinary);
    try {
      ServiceId destinationServiceId = ServiceId.parseOrNull(envelope.destinationServiceId, envelope.destinationServiceIdBinary);
      String    destinationStr       = (destinationServiceId != null) ? destinationServiceId.toString() : "";
      String    serverGuid           = UuidUtil.getStringUUID(envelope.serverGuid, envelope.serverGuidBinary);

      byte[]                paddedMessage;
      ZonaRosaServiceMetadata metadata;
      int                   ciphertextMessageType;

      if (sourceServiceId == null && envelope.type != Envelope.Type.UNIDENTIFIED_SENDER) {
        throw new InvalidMessageStructureException("Non-UD envelope is missing a UUID!");
      }

      if (envelope.type == Envelope.Type.PREKEY_BUNDLE) {
        ZonaRosaProtocolAddress sourceAddress = new ZonaRosaProtocolAddress(sourceServiceId.toString(), envelope.sourceDevice);
        ZonaRosaSessionCipher   sessionCipher = new ZonaRosaSessionCipher(sessionLock, new SessionCipher(zonarosaProtocolStore, sourceAddress));

        paddedMessage         = sessionCipher.decrypt(new PreKeyZonaRosaMessage(envelope.content.toByteArray()));
        metadata              = new ZonaRosaServiceMetadata(getSourceAddress(envelope), envelope.sourceDevice, envelope.timestamp, envelope.serverTimestamp, serverDeliveredTimestamp, false, serverGuid, Optional.empty(), destinationStr);
        ciphertextMessageType = CiphertextMessage.PREKEY_TYPE;

        zonarosaProtocolStore.clearSenderKeySharedWith(Collections.singleton(sourceAddress));
      } else if (envelope.type == Envelope.Type.CIPHERTEXT) {
        ZonaRosaProtocolAddress sourceAddress = new ZonaRosaProtocolAddress(sourceServiceId.toString(), envelope.sourceDevice);
        ZonaRosaSessionCipher   sessionCipher = new ZonaRosaSessionCipher(sessionLock, new SessionCipher(zonarosaProtocolStore, sourceAddress));

        paddedMessage         = sessionCipher.decrypt(new ZonaRosaMessage(envelope.content.toByteArray()));
        metadata              = new ZonaRosaServiceMetadata(getSourceAddress(envelope), envelope.sourceDevice, envelope.timestamp, envelope.serverTimestamp, serverDeliveredTimestamp, false, serverGuid, Optional.empty(), destinationStr);
        ciphertextMessageType = CiphertextMessage.WHISPER_TYPE;
      } else if (envelope.type == Envelope.Type.PLAINTEXT_CONTENT) {
        paddedMessage         = new PlaintextContent(envelope.content.toByteArray()).getBody();
        metadata              = new ZonaRosaServiceMetadata(getSourceAddress(envelope), envelope.sourceDevice, envelope.timestamp, envelope.serverTimestamp, serverDeliveredTimestamp, false, serverGuid, Optional.empty(), destinationStr);
        ciphertextMessageType = CiphertextMessage.PLAINTEXT_CONTENT_TYPE;
      } else if (envelope.type == Envelope.Type.UNIDENTIFIED_SENDER) {
        ZonaRosaSealedSessionCipher sealedSessionCipher = new ZonaRosaSealedSessionCipher(sessionLock, new SealedSessionCipher(zonarosaProtocolStore, localAddress.getServiceId().getRawUuid(), localAddress.getNumber().orElse(null), localDeviceId));
        DecryptionResult          result              = sealedSessionCipher.decrypt(certificateValidator, envelope.content.toByteArray(), envelope.serverTimestamp);
        ZonaRosaServiceAddress      resultAddress       = new ZonaRosaServiceAddress(ACI.parseOrThrow(result.getSenderUuid()), result.getSenderE164());
        Optional<byte[]>          groupId             = result.getGroupId();
        boolean                   needsReceipt        = true;

        if (sourceServiceId != null) {
          Log.w(TAG, "[" + envelope.timestamp + "] Received a UD-encrypted message sent over an identified channel. Marking as needsReceipt=false");
          needsReceipt = false;
        }

        ciphertextMessageType = result.getCiphertextMessageType();

        if (ciphertextMessageType == CiphertextMessage.PREKEY_TYPE) {
          zonarosaProtocolStore.clearSenderKeySharedWith(Collections.singleton(new ZonaRosaProtocolAddress(result.getSenderUuid(), result.getDeviceId())));
        }

        paddedMessage = result.getPaddedMessage();
        metadata      = new ZonaRosaServiceMetadata(resultAddress, result.getDeviceId(), envelope.timestamp, envelope.serverTimestamp, serverDeliveredTimestamp, needsReceipt, serverGuid, groupId, destinationStr);
      } else {
        throw new InvalidMetadataMessageException("Unknown type: " + envelope.type);
      }

      PushTransportDetails transportDetails = new PushTransportDetails();
      byte[]               data             = transportDetails.getStrippedPaddingMessageBody(paddedMessage);

      return new Plaintext(metadata, data, ciphertextMessageType);
    } catch (DuplicateMessageException e) {
      throw new ProtocolDuplicateMessageException(e, sourceServiceId.toString(), envelope.sourceDevice);
    } catch (LegacyMessageException e) {
      throw new ProtocolLegacyMessageException(e, sourceServiceId.toString(), envelope.sourceDevice);
    } catch (InvalidMessageException e) {
      throw new ProtocolInvalidMessageException(e, sourceServiceId.toString(), envelope.sourceDevice);
    } catch (InvalidKeyIdException e) {
      throw new ProtocolInvalidKeyIdException(e, sourceServiceId.toString(), envelope.sourceDevice);
    } catch (InvalidKeyException e) {
      throw new ProtocolInvalidKeyException(e, sourceServiceId.toString(), envelope.sourceDevice);
    } catch (UntrustedIdentityException e) {
      throw new ProtocolUntrustedIdentityException(e, sourceServiceId.toString(), envelope.sourceDevice);
    } catch (InvalidVersionException e) {
      throw new ProtocolInvalidVersionException(e, sourceServiceId.toString(), envelope.sourceDevice);
    } catch (NoSessionException e) {
      throw new ProtocolNoSessionException(e, sourceServiceId.toString(), envelope.sourceDevice);
    }
  }

  private static ZonaRosaServiceAddress getSourceAddress(Envelope envelope) {
    return new ZonaRosaServiceAddress(ServiceId.parseOrNull(envelope.sourceServiceId, envelope.sourceServiceIdBinary));
  }

  private static class Plaintext {
    private final ZonaRosaServiceMetadata metadata;
    private final byte[]                data;
    private final int                   ciphertextMessageType;

    private Plaintext(ZonaRosaServiceMetadata metadata, byte[] data, int ciphertextMessageType) {
      this.metadata              = metadata;
      this.data                  = data;
      this.ciphertextMessageType = ciphertextMessageType;
    }

    public ZonaRosaServiceMetadata getMetadata() {
      return metadata;
    }

    public byte[] getData() {
      return data;
    }

    public int getCiphertextMessageType() {
      return ciphertextMessageType;
    }
  }
}
