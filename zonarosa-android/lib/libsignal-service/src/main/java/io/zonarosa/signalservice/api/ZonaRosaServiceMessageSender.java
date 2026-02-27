/*
 * Copyright (C) 2014-2016 ZonaRosa Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */
package io.zonarosa.service.api;

import io.zonarosa.core.models.ServiceId;
import io.zonarosa.core.models.ServiceId.PNI;
import io.zonarosa.core.util.Base64;
import io.zonarosa.core.util.ProtoUtil;
import io.zonarosa.core.util.UuidUtil;
import io.zonarosa.libzonarosa.metadata.certificate.SenderCertificate;
import io.zonarosa.libzonarosa.protocol.IdentityKey;
import io.zonarosa.libzonarosa.protocol.IdentityKeyPair;
import io.zonarosa.libzonarosa.protocol.InvalidKeyException;
import io.zonarosa.libzonarosa.protocol.InvalidRegistrationIdException;
import io.zonarosa.libzonarosa.protocol.NoSessionException;
import io.zonarosa.libzonarosa.protocol.SessionBuilder;
import io.zonarosa.libzonarosa.protocol.ZonaRosaProtocolAddress;
import io.zonarosa.libzonarosa.protocol.groups.GroupSessionBuilder;
import io.zonarosa.libzonarosa.protocol.logging.Log;
import io.zonarosa.libzonarosa.protocol.message.DecryptionErrorMessage;
import io.zonarosa.libzonarosa.protocol.message.PlaintextContent;
import io.zonarosa.libzonarosa.protocol.message.SenderKeyDistributionMessage;
import io.zonarosa.libzonarosa.protocol.state.PreKeyBundle;
import io.zonarosa.libzonarosa.protocol.state.SessionRecord;
import io.zonarosa.libzonarosa.zkgroup.groupsend.GroupSendFullToken;
import io.zonarosa.service.api.attachment.AttachmentApi;
import io.zonarosa.service.api.crypto.AttachmentCipherStreamUtil;
import io.zonarosa.service.api.crypto.ContentHint;
import io.zonarosa.service.api.crypto.EnvelopeContent;
import io.zonarosa.service.api.crypto.SealedSenderAccess;
import io.zonarosa.service.api.crypto.ZonaRosaGroupSessionBuilder;
import io.zonarosa.service.api.crypto.ZonaRosaServiceCipher;
import io.zonarosa.service.api.crypto.ZonaRosaSessionBuilder;
import io.zonarosa.service.api.crypto.UnidentifiedAccess;
import io.zonarosa.service.api.crypto.UntrustedIdentityException;
import io.zonarosa.service.api.groupsv2.GroupSendEndorsements;
import io.zonarosa.service.api.keys.KeysApi;
import io.zonarosa.service.api.message.MessageApi;
import io.zonarosa.service.api.messages.SendMessageResult;
import io.zonarosa.service.api.messages.ZonaRosaServiceAttachment;
import io.zonarosa.service.api.messages.ZonaRosaServiceAttachmentPointer;
import io.zonarosa.service.api.messages.ZonaRosaServiceAttachmentRemoteId;
import io.zonarosa.service.api.messages.ZonaRosaServiceAttachmentStream;
import io.zonarosa.service.api.messages.ZonaRosaServiceDataMessage;
import io.zonarosa.service.api.messages.ZonaRosaServiceEditMessage;
import io.zonarosa.service.api.messages.ZonaRosaServiceGroupV2;
import io.zonarosa.service.api.messages.ZonaRosaServicePreview;
import io.zonarosa.service.api.messages.ZonaRosaServiceReceiptMessage;
import io.zonarosa.service.api.messages.ZonaRosaServiceStoryMessage;
import io.zonarosa.service.api.messages.ZonaRosaServiceStoryMessageRecipient;
import io.zonarosa.service.api.messages.ZonaRosaServiceTextAttachment;
import io.zonarosa.service.api.messages.ZonaRosaServiceTypingMessage;
import io.zonarosa.service.api.messages.calls.AnswerMessage;
import io.zonarosa.service.api.messages.calls.IceUpdateMessage;
import io.zonarosa.service.api.messages.calls.OfferMessage;
import io.zonarosa.service.api.messages.calls.OpaqueMessage;
import io.zonarosa.service.api.messages.calls.ZonaRosaServiceCallMessage;
import io.zonarosa.service.api.messages.multidevice.BlockedListMessage;
import io.zonarosa.service.api.messages.multidevice.ConfigurationMessage;
import io.zonarosa.service.api.messages.multidevice.KeysMessage;
import io.zonarosa.service.api.messages.multidevice.MessageRequestResponseMessage;
import io.zonarosa.service.api.messages.multidevice.OutgoingPaymentMessage;
import io.zonarosa.service.api.messages.multidevice.ReadMessage;
import io.zonarosa.service.api.messages.multidevice.SentTranscriptMessage;
import io.zonarosa.service.api.messages.multidevice.ZonaRosaServiceSyncMessage;
import io.zonarosa.service.api.messages.multidevice.StickerPackOperationMessage;
import io.zonarosa.service.api.messages.multidevice.VerifiedMessage;
import io.zonarosa.service.api.messages.multidevice.ViewOnceOpenMessage;
import io.zonarosa.service.api.messages.multidevice.ViewedMessage;
import io.zonarosa.service.api.messages.shared.SharedContact;
import io.zonarosa.service.api.push.DistributionId;
import io.zonarosa.service.api.push.ZonaRosaServiceAddress;
import io.zonarosa.service.api.push.exceptions.AuthorizationFailedException;
import io.zonarosa.service.api.push.exceptions.NonSuccessfulResponseCodeException;
import io.zonarosa.service.api.push.exceptions.NotFoundException;
import io.zonarosa.service.api.push.exceptions.ProofRequiredException;
import io.zonarosa.service.api.push.exceptions.PushNetworkException;
import io.zonarosa.service.api.push.exceptions.RateLimitException;
import io.zonarosa.service.api.push.exceptions.ServerRejectedException;
import io.zonarosa.service.api.push.exceptions.UnregisteredUserException;
import io.zonarosa.service.api.util.AttachmentPointerUtil;
import io.zonarosa.service.api.util.CredentialsProvider;
import io.zonarosa.service.api.util.Preconditions;
import io.zonarosa.service.api.util.Uint64RangeException;
import io.zonarosa.service.api.util.Uint64Util;
import io.zonarosa.service.api.websocket.WebSocketUnavailableException;
import io.zonarosa.service.internal.crypto.AttachmentDigest;
import io.zonarosa.service.internal.crypto.PaddingInputStream;
import io.zonarosa.service.internal.push.AttachmentPointer;
import io.zonarosa.service.internal.push.AttachmentUploadForm;
import io.zonarosa.service.internal.push.BodyRange;
import io.zonarosa.service.internal.push.CallMessage;
import io.zonarosa.service.internal.push.Content;
import io.zonarosa.service.internal.push.DataMessage;
import io.zonarosa.service.internal.push.EditMessage;
import io.zonarosa.service.internal.push.GroupContextV2;
import io.zonarosa.service.internal.push.GroupMismatchedDevices;
import io.zonarosa.service.internal.push.GroupStaleDevices;
import io.zonarosa.service.internal.push.MismatchedDevices;
import io.zonarosa.service.internal.push.NullMessage;
import io.zonarosa.service.internal.push.OutgoingPushMessage;
import io.zonarosa.service.internal.push.OutgoingPushMessageList;
import io.zonarosa.service.internal.push.PniSignatureMessage;
import io.zonarosa.service.internal.push.Preview;
import io.zonarosa.service.internal.push.ProvisioningVersion;
import io.zonarosa.service.internal.push.PushAttachmentData;
import io.zonarosa.service.internal.push.PushServiceSocket;
import io.zonarosa.service.internal.push.ReceiptMessage;
import io.zonarosa.service.internal.push.SendGroupMessageResponse;
import io.zonarosa.service.internal.push.SendMessageResponse;
import io.zonarosa.service.internal.push.StaleDevices;
import io.zonarosa.service.internal.push.StoryMessage;
import io.zonarosa.service.internal.push.SyncMessage;
import io.zonarosa.service.internal.push.TextAttachment;
import io.zonarosa.service.internal.push.TypingMessage;
import io.zonarosa.service.internal.push.Verified;
import io.zonarosa.service.internal.push.exceptions.GroupMismatchedDevicesException;
import io.zonarosa.service.internal.push.exceptions.GroupStaleDevicesException;
import io.zonarosa.service.internal.push.exceptions.InvalidUnidentifiedAccessHeaderException;
import io.zonarosa.service.internal.push.exceptions.MismatchedDevicesException;
import io.zonarosa.service.internal.push.exceptions.StaleDevicesException;
import io.zonarosa.service.internal.push.http.AttachmentCipherOutputStreamFactory;
import io.zonarosa.service.internal.push.http.CancelationZonaRosa;
import io.zonarosa.service.internal.push.http.PartialSendBatchCompleteListener;
import io.zonarosa.service.internal.push.http.PartialSendCompleteListener;
import io.zonarosa.service.internal.push.http.ResumableUploadSpec;
import io.zonarosa.service.internal.util.Util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.exceptions.CompositeException;
import io.reactivex.rxjava3.schedulers.Schedulers;
import kotlin.Unit;
import okio.ByteString;
import okio.Utf8;

/**
 * The main interface for sending ZonaRosa Service messages.
 *
 * @author Moxie Marlinspike
 */
public class ZonaRosaServiceMessageSender {

  private static final String TAG = ZonaRosaServiceMessageSender.class.getSimpleName();

  private static final int RETRY_COUNT = 4;

  private final PushServiceSocket             socket;
  private final ZonaRosaServiceAccountDataStore aciStore;
  private final ZonaRosaSessionLock             sessionLock;
  private final ZonaRosaServiceAddress          localAddress;
  private final int                           localDeviceId;
  private final PNI                           localPni;
  private final Optional<EventListener>       eventListener;
  private final IdentityKeyPair               localPniIdentity;

  private final AttachmentApi attachmentApi;
  private final MessageApi    messageApi;
  private final KeysApi       keysApi;

  private final Scheduler       scheduler;
  private final long            maxEnvelopeSize;
  private final BooleanSupplier useRestFallback;
  private final boolean         useBinaryId;
  private final boolean         useStringId;

  public ZonaRosaServiceMessageSender(PushServiceSocket pushServiceSocket,
                                    ZonaRosaServiceDataStore store,
                                    ZonaRosaSessionLock sessionLock,
                                    AttachmentApi attachmentApi,
                                    MessageApi messageApi,
                                    KeysApi keysApi,
                                    Optional<EventListener> eventListener,
                                    ExecutorService executor,
                                    long maxEnvelopeSize,
                                    BooleanSupplier useRestFallback,
                                    boolean useBinaryId,
                                    boolean useStringId)
  {
    CredentialsProvider credentialsProvider = pushServiceSocket.getCredentialsProvider();

    this.socket           = pushServiceSocket;
    this.aciStore         = store.aci();
    this.sessionLock      = sessionLock;
    this.localAddress     = new ZonaRosaServiceAddress(credentialsProvider.getAci(), credentialsProvider.getE164());
    this.localDeviceId    = credentialsProvider.getDeviceId();
    this.localPni         = credentialsProvider.getPni();
    this.attachmentApi    = attachmentApi;
    this.messageApi       = messageApi;
    this.eventListener    = eventListener;
    this.maxEnvelopeSize  = maxEnvelopeSize;
    this.localPniIdentity = store.pni().getIdentityKeyPair();
    this.scheduler        = Schedulers.from(executor, false, false);
    this.keysApi          = keysApi;
    this.useRestFallback  = useRestFallback;
    this.useBinaryId      = useBinaryId;
    this.useStringId      = useStringId;
  }

  /**
   * Send a read receipt for a received message.
   *
   * @param recipient The sender of the received message you're acknowledging.
   * @param message The read receipt to deliver.
   */
  public SendMessageResult sendReceipt(ZonaRosaServiceAddress recipient,
                                       @Nullable SealedSenderAccess sealedSenderAccess,
                                       ZonaRosaServiceReceiptMessage message,
                                       boolean includePniSignature)
      throws IOException, UntrustedIdentityException
  {
    Log.d(TAG, "[" + message.getWhen() + "] Sending a receipt.");

    Content content = createReceiptContent(message);

    if (includePniSignature) {
      content = content.newBuilder()
                       .pniSignatureMessage(createPniSignatureMessage())
                       .build();
    }

    EnvelopeContent envelopeContent = EnvelopeContent.encrypted(content, ContentHint.IMPLICIT, Optional.empty());

    return sendMessage(recipient, sealedSenderAccess, message.getWhen(), envelopeContent, false, null, null, false, false);
  }

  /**
   * Send a retry receipt for a bad-encrypted envelope.
   */
  public void sendRetryReceipt(ZonaRosaServiceAddress recipient,
                               @Nullable SealedSenderAccess sealedSenderAccess,
                               Optional<byte[]> groupId,
                               DecryptionErrorMessage errorMessage)
      throws IOException, UntrustedIdentityException

  {
    long timestamp = System.currentTimeMillis();

    Log.d(TAG, "[" + timestamp + "] Sending a retry receipt for target message " + errorMessage.getTimestamp());

    PlaintextContent content         = new PlaintextContent(errorMessage);
    EnvelopeContent  envelopeContent = EnvelopeContent.plaintext(content, groupId);

    sendMessage(recipient, sealedSenderAccess, timestamp, envelopeContent, false, null, null, false, false);
  }

  /**
   * Sends a typing indicator using client-side fanout. Doesn't bother with return results, since these are best-effort.
   */
  public void sendTyping(List<ZonaRosaServiceAddress> recipients,
                         List<SealedSenderAccess> sealedSenderAccesses,
                         ZonaRosaServiceTypingMessage message,
                         CancelationZonaRosa cancelationZonaRosa)
      throws IOException
  {
    Log.d(TAG, "[" + message.getTimestamp() + "] Sending a typing message to " + recipients.size() + " recipient(s) using 1:1 messages.");

    Content         content         = createTypingContent(message);
    EnvelopeContent envelopeContent = EnvelopeContent.encrypted(content, ContentHint.IMPLICIT, Optional.empty());

    sendMessage(recipients, sealedSenderAccesses, message.getTimestamp(), envelopeContent, true, null, cancelationZonaRosa, null, false, false);
  }

  /**
   * Send a typing indicator to a group using sender key. Doesn't bother with return results, since these are best-effort.
   */
  public void sendGroupTyping(DistributionId distributionId,
                              List<ZonaRosaServiceAddress> recipients,
                              List<UnidentifiedAccess> unidentifiedAccess,
                              @Nonnull GroupSendEndorsements groupSendEndorsements,
                              ZonaRosaServiceTypingMessage message)
      throws IOException, UntrustedIdentityException, InvalidKeyException, NoSessionException, InvalidRegistrationIdException
  {
    Log.d(TAG, "[" + message.getTimestamp() + "] Sending a typing message to " + recipients.size() + " recipient(s) using sender key.");

    Content content = createTypingContent(message);
    sendGroupMessage(distributionId, recipients, unidentifiedAccess, groupSendEndorsements, message.getTimestamp(), content, ContentHint.IMPLICIT, message.getGroupId(), true, SenderKeyGroupEvents.EMPTY, false, false);
  }

  /**
   * Only sends sync message for a story. Useful if you're sending to a group with no one else in it -- meaning you don't need to send a story, but you do need
   * to send it to your linked devices.
   */
  public void sendStorySyncMessage(ZonaRosaServiceStoryMessage message,
                                   long timestamp,
                                   boolean isRecipientUpdate,
                                   Set<ZonaRosaServiceStoryMessageRecipient> manifest)
      throws IOException, UntrustedIdentityException
  {
    Log.d(TAG, "[" + timestamp + "] Sending a story sync message.");

    if (manifest.isEmpty() && !message.getGroupContext().isPresent()) {
      Log.w(TAG, "Refusing to send sync message for empty manifest in non-group story.");
      return;
    }

    ZonaRosaServiceSyncMessage syncMessage = createSelfSendSyncMessageForStory(message, timestamp, isRecipientUpdate, manifest);
    sendSyncMessage(syncMessage);
  }

  /**
   * Send a story using sender key. Note: This is not just for group stories -- it's for any story. Just following the naming convention of making sender key
   * method named "sendGroup*"
   */
  public List<SendMessageResult> sendGroupStory(DistributionId distributionId,
                                                Optional<byte[]> groupId,
                                                List<ZonaRosaServiceAddress> recipients,
                                                List<UnidentifiedAccess> unidentifiedAccess,
                                                @Nullable GroupSendEndorsements groupSendEndorsements,
                                                boolean isRecipientUpdate,
                                                ZonaRosaServiceStoryMessage message,
                                                long timestamp,
                                                Set<ZonaRosaServiceStoryMessageRecipient> manifest,
                                                PartialSendBatchCompleteListener partialListener)
      throws IOException, UntrustedIdentityException, InvalidKeyException, NoSessionException, InvalidRegistrationIdException
  {
    Log.d(TAG, "[" + timestamp + "] Sending a story.");

    Content                  content            = createStoryContent(message);
    List<SendMessageResult>  sendMessageResults = sendGroupMessage(distributionId, recipients, unidentifiedAccess, groupSendEndorsements, timestamp, content, ContentHint.IMPLICIT, groupId, false, SenderKeyGroupEvents.EMPTY, false, true);

    if (partialListener != null) {
      partialListener.onPartialSendComplete(sendMessageResults);
    }

    if (aciStore.isMultiDevice()) {
      sendStorySyncMessage(message, timestamp, isRecipientUpdate, manifest);
    }

    return sendMessageResults;
  }


  /**
   * Send a call setup message to a single recipient.
   *
   * @param recipient The message's destination.
   * @param message The call message.
   * @throws IOException
   */
  public void sendCallMessage(ZonaRosaServiceAddress recipient,
                              @Nullable SealedSenderAccess sealedSenderAccess,
                              ZonaRosaServiceCallMessage message)
      throws IOException, UntrustedIdentityException
  {
    long timestamp = System.currentTimeMillis();
    Log.d(TAG, "[" + timestamp + "] Sending a call message (single recipient).");

    Content         content         = createCallContent(message);
    EnvelopeContent envelopeContent = EnvelopeContent.encrypted(content, ContentHint.DEFAULT, Optional.empty());

    sendMessage(recipient, sealedSenderAccess, timestamp, envelopeContent, false, null, null, message.isUrgent(), false);
  }

  public List<SendMessageResult> sendCallMessage(List<ZonaRosaServiceAddress> recipients,
                                                 List<SealedSenderAccess> sealedSenderAccesses,
                                                 ZonaRosaServiceCallMessage message)
      throws IOException
  {
    long timestamp = System.currentTimeMillis();
    Log.d(TAG, "[" + timestamp + "] Sending a call message (multiple recipients).");

    Content         content         = createCallContent(message);
    EnvelopeContent envelopeContent = EnvelopeContent.encrypted(content, ContentHint.DEFAULT, Optional.empty());

    return sendMessage(recipients, sealedSenderAccesses, timestamp, envelopeContent, false, null, null, null, message.isUrgent(), false);
  }

  public List<SendMessageResult> sendCallMessage(DistributionId distributionId,
                                                 List<ZonaRosaServiceAddress> recipients,
                                                 List<UnidentifiedAccess> unidentifiedAccess,
                                                 @Nonnull GroupSendEndorsements groupSendEndorsements,
                                                 ZonaRosaServiceCallMessage message,
                                                 PartialSendBatchCompleteListener partialListener)
      throws IOException, UntrustedIdentityException, InvalidKeyException, NoSessionException, InvalidRegistrationIdException
  {
    Log.d(TAG, "[" + message.getTimestamp().get() + "] Sending a call message (sender key).");

    Content content = createCallContent(message);

    List<SendMessageResult> results = sendGroupMessage(distributionId, recipients, unidentifiedAccess, groupSendEndorsements, message.getTimestamp().get(), content, ContentHint.IMPLICIT, message.getGroupId(), false, SenderKeyGroupEvents.EMPTY, message.isUrgent(), false);

    if (partialListener != null) {
      partialListener.onPartialSendComplete(results);
    }

    return results;
  }

  /**
   * Send a message to a single recipient.
   *
   * @param recipient The message's destination.
   * @param message The message.
   * @throws UntrustedIdentityException
   * @throws IOException
   */
  public SendMessageResult sendDataMessage(ZonaRosaServiceAddress recipient,
                                           @Nullable SealedSenderAccess sealedSenderAccess,
                                           ContentHint contentHint,
                                           ZonaRosaServiceDataMessage message,
                                           IndividualSendEvents sendEvents,
                                           boolean urgent,
                                           boolean includePniSignature)
      throws UntrustedIdentityException, IOException
  {
    Log.d(TAG, "[" + message.getTimestamp() + "] Sending a data message.");

    Content content = createMessageContent(message);

    return sendContent(recipient, sealedSenderAccess, contentHint, message, sendEvents, urgent, includePniSignature, content);
  }

  /**
   * Send an edit message to a single recipient.
   */
  public SendMessageResult sendEditMessage(ZonaRosaServiceAddress recipient,
                                           @Nullable SealedSenderAccess sealedSenderAccess,
                                           ContentHint contentHint,
                                           ZonaRosaServiceDataMessage message,
                                           IndividualSendEvents sendEvents,
                                           boolean urgent,
                                           long targetSentTimestamp)
      throws UntrustedIdentityException, IOException
  {
    Log.d(TAG, "[" + message.getTimestamp() + "] Sending an edit message for " + targetSentTimestamp + ".");

    Content content = createEditMessageContent(new ZonaRosaServiceEditMessage(targetSentTimestamp, message));

    return sendContent(recipient, sealedSenderAccess, contentHint, message, sendEvents, urgent, false, content);
  }

  /**
   * Sends content to a single recipient.
   */
  private SendMessageResult sendContent(ZonaRosaServiceAddress recipient,
                                        @Nullable SealedSenderAccess sealedSenderAccess,
                                        ContentHint contentHint,
                                        ZonaRosaServiceDataMessage message,
                                        IndividualSendEvents sendEvents,
                                        boolean urgent,
                                        boolean includePniSignature,
                                        Content content)
      throws UntrustedIdentityException, IOException
  {
    if (includePniSignature) {
      Log.d(TAG, "[" + message.getTimestamp() + "] Including PNI signature.");
      content = content.newBuilder()
                       .pniSignatureMessage(createPniSignatureMessage())
                       .build();
    }

    EnvelopeContent envelopeContent = EnvelopeContent.encrypted(content, contentHint, message.getGroupId());

    long              timestamp = message.getTimestamp();
    SendMessageResult result    = sendMessage(recipient, sealedSenderAccess, timestamp, envelopeContent, false, null, sendEvents, urgent, false);

    sendEvents.onMessageSent();

    if (result.getSuccess() != null && result.getSuccess().isNeedsSync()) {
      Content         syncMessage        = createMultiDeviceSentTranscriptContent(content, Optional.of(recipient), timestamp, Collections.singletonList(result), false, Collections.emptySet());
      EnvelopeContent syncMessageContent = EnvelopeContent.encrypted(syncMessage, ContentHint.IMPLICIT, Optional.empty());

      sendMessage(localAddress, SealedSenderAccess.NONE, timestamp, syncMessageContent, false, null, null, false, false);
    }

    sendEvents.onSyncMessageSent();

    return result;
  }

  /**
   * Gives you a {@link SenderKeyDistributionMessage} that can then be sent out to recipients to tell them about your sender key.
   * Will create a sender key session for the provided DistributionId if one doesn't exist.
   */
  public SenderKeyDistributionMessage getOrCreateNewGroupSession(DistributionId distributionId) {
    ZonaRosaProtocolAddress self = new ZonaRosaProtocolAddress(localAddress.getIdentifier(), localDeviceId);
    return new ZonaRosaGroupSessionBuilder(sessionLock, new GroupSessionBuilder(aciStore)).create(self, distributionId.asUuid());
  }

  /**
   * Sends the provided {@link SenderKeyDistributionMessage} to the specified recipients.
   */
  public List<SendMessageResult> sendSenderKeyDistributionMessage(DistributionId distributionId,
                                                                  List<ZonaRosaServiceAddress> recipients,
                                                                  List<SealedSenderAccess> sealedSenderAccesses,
                                                                  SenderKeyDistributionMessage message,
                                                                  Optional<byte[]> groupId,
                                                                  boolean urgent,
                                                                  boolean story)
      throws IOException
  {
    ByteString      distributionBytes = ByteString.of(message.serialize());
    Content         content           = new Content.Builder().senderKeyDistributionMessage(distributionBytes).build();
    EnvelopeContent envelopeContent   = EnvelopeContent.encrypted(content, ContentHint.IMPLICIT, groupId);
    long            timestamp         = System.currentTimeMillis();

    Log.d(TAG, "[" + timestamp + "] Sending SKDM to " + recipients.size() + " recipients for DistributionId " + distributionId);

    return sendMessage(recipients, sealedSenderAccesses, timestamp, envelopeContent, false, null, null, null, urgent, story);
  }

  /**
   * Resend a previously-sent message.
   */
  public SendMessageResult resendContent(ZonaRosaServiceAddress address,
                                         @Nullable SealedSenderAccess sealedSenderAccess,
                                         long timestamp,
                                         Content content,
                                         ContentHint contentHint,
                                         Optional<byte[]> groupId,
                                         boolean urgent)
      throws UntrustedIdentityException, IOException
  {
    Log.d(TAG, "[" + timestamp + "] Resending content.");

    EnvelopeContent              envelopeContent = EnvelopeContent.encrypted(content, contentHint, groupId);

    if (address.getServiceId().equals(localAddress.getServiceId())) {
      sealedSenderAccess = SealedSenderAccess.NONE;
    }

    return sendMessage(address, sealedSenderAccess, timestamp, envelopeContent, false, null, null, urgent, false);
  }

  /**
   * Sends a {@link ZonaRosaServiceDataMessage} to a group using sender keys.
   */
  public List<SendMessageResult> sendGroupDataMessage(DistributionId distributionId,
                                                      List<ZonaRosaServiceAddress> recipients,
                                                      List<UnidentifiedAccess> unidentifiedAccess,
                                                      @Nullable GroupSendEndorsements groupSendEndorsements,
                                                      boolean isRecipientUpdate,
                                                      ContentHint contentHint,
                                                      ZonaRosaServiceDataMessage message,
                                                      SenderKeyGroupEvents sendEvents,
                                                      boolean urgent,
                                                      boolean isForStory,
                                                      ZonaRosaServiceEditMessage editMessage,
                                                      PartialSendBatchCompleteListener partialListener)
      throws IOException, UntrustedIdentityException, NoSessionException, InvalidKeyException, InvalidRegistrationIdException
  {
    Log.d(TAG, "[" + message.getTimestamp() + "] Sending a group " + (editMessage != null ? "edit data message" : "data message") + " to " + recipients.size() + " recipients using DistributionId " + distributionId);

    Content content;

    if (editMessage != null) {
      content = createEditMessageContent(editMessage);
    } else {
      content = createMessageContent(message);
    }

    Optional<byte[]>        groupId = message.getGroupId();
    List<SendMessageResult> results = sendGroupMessage(distributionId, recipients, unidentifiedAccess, groupSendEndorsements, message.getTimestamp(), content, contentHint, groupId, false, sendEvents, urgent, isForStory);

    if (partialListener != null) {
      partialListener.onPartialSendComplete(results);
    }

    sendEvents.onMessageSent();

    if (aciStore.isMultiDevice()) {
      Content         syncMessage        = createMultiDeviceSentTranscriptContent(content, Optional.empty(), message.getTimestamp(), results, isRecipientUpdate, Collections.emptySet());
      EnvelopeContent syncMessageContent = EnvelopeContent.encrypted(syncMessage, ContentHint.IMPLICIT, Optional.empty());

      sendMessage(localAddress, SealedSenderAccess.NONE, message.getTimestamp(), syncMessageContent, false, null, null, false, false);
    }

    sendEvents.onSyncMessageSent();

    return results;
  }

  /**
   * Sends a message to a group using client-side fanout.
   *
   * @param partialListener A listener that will be called when an individual send is completed. Will be invoked on an arbitrary background thread, *not*
   *                        the calling thread.
   */
  public List<SendMessageResult> sendDataMessage(List<ZonaRosaServiceAddress> recipients,
                                                 List<SealedSenderAccess> sealedSenderAccesses,
                                                 boolean isRecipientUpdate,
                                                 ContentHint contentHint,
                                                 ZonaRosaServiceDataMessage message,
                                                 LegacyGroupEvents sendEvents,
                                                 PartialSendCompleteListener partialListener,
                                                 CancelationZonaRosa cancelationZonaRosa,
                                                 boolean urgent)
      throws IOException, UntrustedIdentityException
  {
    Log.d(TAG, "[" + message.getTimestamp() + "] Sending a data message to " + recipients.size() + " recipients.");

    Content                 content            = createMessageContent(message);
    EnvelopeContent         envelopeContent    = EnvelopeContent.encrypted(content, contentHint, message.getGroupId());
    long                    timestamp          = message.getTimestamp();
    List<SendMessageResult> results            = sendMessage(recipients, sealedSenderAccesses, timestamp, envelopeContent, false, partialListener, cancelationZonaRosa, sendEvents, urgent, false);
    boolean                 needsSyncInResults = false;

    sendEvents.onMessageSent();

    for (SendMessageResult result : results) {
      if (result.getSuccess() != null && result.getSuccess().isNeedsSync()) {
        needsSyncInResults = true;
        break;
      }
    }

    if (needsSyncInResults || aciStore.isMultiDevice()) {
      Optional<ZonaRosaServiceAddress> recipient = Optional.empty();
      if (!message.getGroupContext().isPresent() && recipients.size() == 1) {
        recipient = Optional.of(recipients.get(0));
      }

      Content         syncMessage        = createMultiDeviceSentTranscriptContent(content, recipient, timestamp, results, isRecipientUpdate, Collections.emptySet());
      EnvelopeContent syncMessageContent = EnvelopeContent.encrypted(syncMessage, ContentHint.IMPLICIT, Optional.empty());

      sendMessage(localAddress, SealedSenderAccess.NONE, timestamp, syncMessageContent, false, null, null, false, false);
    }

    sendEvents.onSyncMessageSent();

    return results;
  }

  /**
   * Sends an edit message to a group using client-side fanout.
   *
   * @param partialListener A listener that will be called when an individual send is completed. Will be invoked on an arbitrary background thread, *not*
   *                        the calling thread.
   */
  public List<SendMessageResult> sendEditMessage(List<ZonaRosaServiceAddress> recipients,
                                                 List<SealedSenderAccess> sealedSenderAccesses,
                                                 boolean isRecipientUpdate,
                                                 ContentHint contentHint,
                                                 ZonaRosaServiceDataMessage message,
                                                 LegacyGroupEvents sendEvents,
                                                 PartialSendCompleteListener partialListener,
                                                 CancelationZonaRosa cancelationZonaRosa,
                                                 boolean urgent,
                                                 long targetSentTimestamp)
      throws IOException, UntrustedIdentityException
  {
    Log.d(TAG, "[" + message.getTimestamp() + "] Sending a edit message to " + recipients.size() + " recipients.");

    Content                 content            = createEditMessageContent(new ZonaRosaServiceEditMessage(targetSentTimestamp, message));
    EnvelopeContent         envelopeContent    = EnvelopeContent.encrypted(content, contentHint, message.getGroupId());
    long                    timestamp          = message.getTimestamp();
    List<SendMessageResult> results            = sendMessage(recipients, sealedSenderAccesses, timestamp, envelopeContent, false, partialListener, cancelationZonaRosa, null, urgent, false);
    boolean                 needsSyncInResults = false;

    sendEvents.onMessageSent();

    for (SendMessageResult result : results) {
      if (result.getSuccess() != null && result.getSuccess().isNeedsSync()) {
        needsSyncInResults = true;
        break;
      }
    }

    if (needsSyncInResults || aciStore.isMultiDevice()) {
      Optional<ZonaRosaServiceAddress> recipient = Optional.empty();
      if (!message.getGroupContext().isPresent() && recipients.size() == 1) {
        recipient = Optional.of(recipients.get(0));
      }

      Content         syncMessage        = createMultiDeviceSentTranscriptContent(content, recipient, timestamp, results, isRecipientUpdate, Collections.emptySet());
      EnvelopeContent syncMessageContent = EnvelopeContent.encrypted(syncMessage, ContentHint.IMPLICIT, Optional.empty());

      sendMessage(localAddress, SealedSenderAccess.NONE, timestamp, syncMessageContent, false, null, null, false, false);
    }

    sendEvents.onSyncMessageSent();

    return results;
  }

  public SendMessageResult sendSyncMessage(ZonaRosaServiceDataMessage dataMessage)
      throws IOException, UntrustedIdentityException
  {
    Log.d(TAG, "[" + dataMessage.getTimestamp() + "] Sending self-sync message.");
    return sendSyncMessage(createSelfSendSyncMessage(dataMessage));
  }

  public SendMessageResult sendSelfSyncEditMessage(ZonaRosaServiceEditMessage editMessage)
      throws IOException, UntrustedIdentityException
  {
    Log.d(TAG, "[" + editMessage.getDataMessage().getTimestamp() + "] Sending self-sync edit message for " + editMessage.getTargetSentTimestamp() + ".");
    return sendSyncMessage(createSelfSendSyncEditMessage(editMessage));
  }

  public SendMessageResult sendSyncMessage(ZonaRosaServiceSyncMessage message)
      throws IOException, UntrustedIdentityException
  {
    Content content;
    boolean urgent = false;

    if (!aciStore.isMultiDevice()) {
      Log.d(TAG, "We do not have any linked devices. Skipping.");
      return SendMessageResult.success(localAddress, Collections.emptyList(), false, false, 0, Optional.empty());
    }

    if (message.getContacts().isPresent()) {
      content = createMultiDeviceContactsContent(message.getContacts().get().getContactsStream().asStream(), message.getContacts().get().isComplete());
    } else if (message.getRead().isPresent()) {
      content = createMultiDeviceReadContent(message.getRead().get());
      urgent  = true;
    } else if (message.getViewed().isPresent()) {
      content = createMultiDeviceViewedContent(message.getViewed().get());
    } else if (message.getViewOnceOpen().isPresent()) {
      content = createMultiDeviceViewOnceOpenContent(message.getViewOnceOpen().get());
    } else if (message.getBlockedList().isPresent()) {
      content = createMultiDeviceBlockedContent(message.getBlockedList().get());
    } else if (message.getConfiguration().isPresent()) {
      content = createMultiDeviceConfigurationContent(message.getConfiguration().get());
    } else if (message.getSent().isPresent()) {
      content = createMultiDeviceSentTranscriptContent(message.getSent().get());
    } else if (message.getStickerPackOperations().isPresent()) {
      content = createMultiDeviceStickerPackOperationContent(message.getStickerPackOperations().get());
    } else if (message.getFetchType().isPresent()) {
      content = createMultiDeviceFetchTypeContent(message.getFetchType().get());
    } else if (message.getMessageRequestResponse().isPresent()) {
      content = createMultiDeviceMessageRequestResponseContent(message.getMessageRequestResponse().get());
    } else if (message.getOutgoingPaymentMessage().isPresent()) {
      content = createMultiDeviceOutgoingPaymentContent(message.getOutgoingPaymentMessage().get());
    } else if (message.getKeys().isPresent()) {
      content = createMultiDeviceSyncKeysContent(message.getKeys().get());
    } else if (message.getVerified().isPresent()) {
      return sendVerifiedSyncMessage(message.getVerified().get());
    } else if (message.getRequest().isPresent()) {
      content = createRequestContent(message.getRequest().get().getRequest());
      urgent  = message.getRequest().get().isUrgent();
    } else if (message.getCallEvent().isPresent()) {
      content = createCallEventContent(message.getCallEvent().get());
    } else if (message.getCallLinkUpdate().isPresent()) {
      content = createCallLinkUpdateContent(message.getCallLinkUpdate().get());
    } else if (message.getCallLogEvent().isPresent()) {
      content = createCallLogEventContent(message.getCallLogEvent().get());
    } else if (message.getDeviceNameChange().isPresent()) {
      content = createDeviceNameChangeContent(message.getDeviceNameChange().get());
    } else if (message.getAttachmentBackfillResponse().isPresent()) {
      content = createAttachmentBackfillResponseContent(message.getAttachmentBackfillResponse().get());
    } else {
      throw new IOException("Unsupported sync message!");
    }

    Optional<Long> timestamp = message.getSent().map(SentTranscriptMessage::getTimestamp);

    return sendSyncMessage(content, urgent, timestamp);
  }

  public @Nonnull SendMessageResult sendSyncMessage(Content content, boolean urgent, Optional<Long> sent)
      throws IOException, UntrustedIdentityException
  {
    long timestamp = sent.orElseGet(System::currentTimeMillis);

    EnvelopeContent envelopeContent = EnvelopeContent.encrypted(content, ContentHint.IMPLICIT, Optional.empty());

    return sendMessage(localAddress, SealedSenderAccess.NONE, timestamp, envelopeContent, false, null, null, urgent, false);
  }

  /**
   * Create a device specific sync message that includes updated PNI details for that specific linked device. This message is
   * sent to the server via the change number endpoint and not the normal sync message sending flow.
   *
   * @param deviceId - Device ID of linked device to build message for
   * @param pniChangeNumber - Linked device specific updated PNI details
   * @return Encrypted {@link OutgoingPushMessage} to be included in the change number request sent to the server
   */
  public @Nonnull OutgoingPushMessage getEncryptedSyncPniInitializeDeviceMessage(int deviceId, @Nonnull SyncMessage.PniChangeNumber pniChangeNumber)
      throws UntrustedIdentityException, IOException, InvalidKeyException
  {
    SyncMessage.Builder syncMessage     = createSyncMessageBuilder().pniChangeNumber(pniChangeNumber);
    Content.Builder     content         = new Content.Builder().syncMessage(syncMessage.build());
    EnvelopeContent     envelopeContent = EnvelopeContent.encrypted(content.build(), ContentHint.IMPLICIT, Optional.empty());

    return getEncryptedMessage(localAddress, SealedSenderAccess.NONE, deviceId, envelopeContent, false);
  }

  public void cancelInFlightRequests() {
    socket.cancelInFlightRequests();
  }

  public ZonaRosaServiceAttachmentPointer uploadAttachment(ZonaRosaServiceAttachmentStream attachment) throws IOException {
    byte[]             attachmentKey    = attachment.getResumableUploadSpec().map(ResumableUploadSpec::getAttachmentKey).orElseGet(() -> Util.getSecretBytes(64));
    byte[]             attachmentIV     = attachment.getResumableUploadSpec().map(ResumableUploadSpec::getAttachmentIv).orElseGet(() -> Util.getSecretBytes(16));
    long               paddedLength     = PaddingInputStream.getPaddedSize(attachment.getLength());
    InputStream        dataStream       = new PaddingInputStream(attachment.getInputStream(), attachment.getLength());
    long               ciphertextLength = AttachmentCipherStreamUtil.getCiphertextLength(paddedLength);
    PushAttachmentData attachmentData   = new PushAttachmentData(attachment.getContentType(),
                                                                 dataStream,
                                                                 ciphertextLength,
                                                                 attachment.isFaststart(),
                                                                 new AttachmentCipherOutputStreamFactory(attachmentKey, attachmentIV),
                                                                 attachment.getListener(),
                                                                 attachment.getCancelationZonaRosa(),
                                                                 attachment.getResumableUploadSpec().get());

    if (attachment.getResumableUploadSpec().isEmpty()) {
      throw new IllegalStateException("Attachment must have a resumable upload spec.");
    }

    return uploadAttachmentV4(attachment, attachmentKey, attachmentData);
  }

  public ResumableUploadSpec getResumableUploadSpec() throws IOException {
    Log.d(TAG, "Using pipe to retrieve attachment upload attributes...");
    AttachmentUploadForm v4UploadAttributes = NetworkResultUtil.toBasicLegacy(attachmentApi.getAttachmentV4UploadForm());

    return socket.getResumableUploadSpec(v4UploadAttributes);
  }

  private ZonaRosaServiceAttachmentPointer uploadAttachmentV4(ZonaRosaServiceAttachmentStream attachment, byte[] attachmentKey, PushAttachmentData attachmentData) throws IOException {
    AttachmentDigest digest = socket.uploadAttachment(attachmentData);
    return new ZonaRosaServiceAttachmentPointer(attachmentData.getResumableUploadSpec().getCdnNumber(),
                                              new ZonaRosaServiceAttachmentRemoteId.V4(attachmentData.getResumableUploadSpec().getCdnKey()),
                                              attachment.getContentType(),
                                              attachmentKey,
                                              Optional.of(Util.toIntExact(attachment.getLength())),
                                              attachment.getPreview(),
                                              attachment.getWidth(),
                                              attachment.getHeight(),
                                              Optional.of(digest.getDigest()),
                                              Optional.ofNullable(digest.getIncrementalDigest()),
                                              digest.getIncrementalDigest() != null ? digest.getIncrementalMacChunkSize() : 0,
                                              attachment.getFileName(),
                                              attachment.getVoiceNote(),
                                              attachment.isBorderless(),
                                              attachment.isGif(),
                                              attachment.getCaption(),
                                              attachment.getBlurHash(),
                                              attachment.getUploadTimestamp(),
                                              attachment.getUuid());
  }

  private SendMessageResult sendVerifiedSyncMessage(VerifiedMessage message)
      throws IOException, UntrustedIdentityException
  {
    byte[] nullMessageBody = new DataMessage.Builder()
                                            .body(Base64.encodeWithPadding(Util.getRandomLengthSecretBytes(140)))
                                            .build()
                                            .encode();

    NullMessage nullMessage = new NullMessage.Builder()
                                             .padding(ByteString.of(nullMessageBody))
                                             .build();

    Content     content     = new Content.Builder()
                                         .nullMessage(nullMessage)
                                         .build();

    EnvelopeContent envelopeContent = EnvelopeContent.encrypted(content, ContentHint.IMPLICIT, Optional.empty());

    SendMessageResult result = sendMessage(message.getDestination(), null, message.getTimestamp(), envelopeContent, false, null, null, false, false);

    if (result.getSuccess().isNeedsSync()) {
      Content         syncMessage        = createMultiDeviceVerifiedContent(message, nullMessage.encode());
      EnvelopeContent syncMessageContent = EnvelopeContent.encrypted(syncMessage, ContentHint.IMPLICIT, Optional.empty());

      sendMessage(localAddress, SealedSenderAccess.NONE, message.getTimestamp(), syncMessageContent, false, null, null, false, false);
    }

    return result;
  }

  public SendMessageResult sendNullMessage(ZonaRosaServiceAddress address, @Nullable SealedSenderAccess sealedSenderAccess)
      throws UntrustedIdentityException, IOException
  {
    byte[] nullMessageBody = new DataMessage.Builder()
                                            .body(Base64.encodeWithPadding(Util.getRandomLengthSecretBytes(140)))
                                            .build()
                                            .encode();

    NullMessage nullMessage = new NullMessage.Builder()
                                             .padding(ByteString.of(nullMessageBody))
                                             .build();

    Content     content     = new Content.Builder()
                                         .nullMessage(nullMessage)
                                         .build();

    EnvelopeContent envelopeContent = EnvelopeContent.encrypted(content, ContentHint.IMPLICIT, Optional.empty());

    return sendMessage(address, sealedSenderAccess, System.currentTimeMillis(), envelopeContent, false, null, null, false, false);
  }

  private PniSignatureMessage createPniSignatureMessage() {
    byte[] signature = localPniIdentity.signAlternateIdentity(aciStore.getIdentityKeyPair().getPublicKey());

    return new PniSignatureMessage.Builder()
                                  .pni(UuidUtil.toByteString(localPni.getRawUuid()))
                                  .signature(ByteString.of(signature))
                                  .build();
  }

  private Content createTypingContent(ZonaRosaServiceTypingMessage message) {
    Content.Builder       container = new Content.Builder();
    TypingMessage.Builder builder   = new TypingMessage.Builder();

    builder.timestamp(message.getTimestamp());

    if      (message.isTypingStarted()) builder.action(TypingMessage.Action.STARTED);
    else if (message.isTypingStopped()) builder.action(TypingMessage.Action.STOPPED);
    else                                throw new IllegalArgumentException("Unknown typing indicator");

    if (message.getGroupId().isPresent()) {
      builder.groupId(ByteString.of(message.getGroupId().get()));
    }

    return container.typingMessage(builder.build()).build();
  }

  private Content createStoryContent(ZonaRosaServiceStoryMessage message) throws IOException {
    Content.Builder      container = new Content.Builder();
    StoryMessage.Builder builder   = new StoryMessage.Builder();

    if (message.getProfileKey().isPresent()) {
      builder.profileKey(ByteString.of(message.getProfileKey().get()));
    }

    if (message.getGroupContext().isPresent()) {
      builder.group(createGroupContent(message.getGroupContext().get()));
    }

    if (message.getFileAttachment().isPresent()) {
      if (message.getFileAttachment().get().isStream()) {
        builder.fileAttachment(createAttachmentPointer(message.getFileAttachment().get().asStream()));
      } else {
        builder.fileAttachment(createAttachmentPointer(message.getFileAttachment().get().asPointer()));
      }
    }

    if (message.getTextAttachment().isPresent()) {
      builder.textAttachment(createTextAttachment(message.getTextAttachment().get()));
    }

    if (message.getBodyRanges().isPresent()) {
      builder.bodyRanges(message.getBodyRanges().get());
    }

    builder.allowsReplies(message.getAllowsReplies().orElse(true));

    return container.storyMessage(builder.build()).build();
  }

  private Content createReceiptContent(ZonaRosaServiceReceiptMessage message) {
    Content.Builder        container = new Content.Builder();
    ReceiptMessage.Builder builder   = new ReceiptMessage.Builder();

    builder.timestamp = message.getTimestamps();

    if      (message.isDeliveryReceipt()) builder.type(ReceiptMessage.Type.DELIVERY);
    else if (message.isReadReceipt())     builder.type(ReceiptMessage.Type.READ);
    else if (message.isViewedReceipt())   builder.type(ReceiptMessage.Type.VIEWED);

    return container.receiptMessage(builder.build()).build();
  }

  private Content createMessageContent(SentTranscriptMessage transcriptMessage) throws IOException {
    if (transcriptMessage.getStoryMessage().isPresent()) {
      return createStoryContent(transcriptMessage.getStoryMessage().get());
    } else if (transcriptMessage.getDataMessage().isPresent()) {
      return createMessageContent(transcriptMessage.getDataMessage().get());
    } else if (transcriptMessage.getEditMessage().isPresent()) {
      return createEditMessageContent(transcriptMessage.getEditMessage().get());
    } else {
      return null;
    }
  }

  private Content createMessageContent(ZonaRosaServiceDataMessage message) throws IOException {
    Content.Builder     container   = new Content.Builder();
    DataMessage.Builder dataMessage = createDataMessage(message);

    if (dataMessage.body != null && Utf8.size(dataMessage.body) > 2048) {
      throw new ContentTooLargeException(Utf8.size(dataMessage.body), "UTF-8 size of the data message body was over 2048 bytes!");
    }

    return enforceMaxContentSize(container.dataMessage(dataMessage.build()).build());
  }

  private Content createEditMessageContent(ZonaRosaServiceEditMessage editMessage) throws IOException {
    Content.Builder     container        = new Content.Builder();
    DataMessage.Builder dataMessage      = createDataMessage(editMessage.getDataMessage());
    EditMessage.Builder editMessageProto = new EditMessage.Builder()
                                                          .dataMessage(dataMessage.build())
                                                          .targetSentTimestamp(editMessage.getTargetSentTimestamp());

    return enforceMaxContentSize(container.editMessage(editMessageProto.build()).build());
  }

  private DataMessage.Builder createDataMessage(ZonaRosaServiceDataMessage message) throws IOException {
    DataMessage.Builder     builder  = new DataMessage.Builder();
    List<AttachmentPointer> pointers = createAttachmentPointers(message.getAttachments());

    builder.requiredProtocolVersion = 0;

    if (!pointers.isEmpty()) {
      builder.attachments(pointers);

      for (AttachmentPointer pointer : pointers) {
        // TODO [cody] wire
//        if (pointer.getAttachmentIdentifierCase() == AttachmentPointer.AttachmentIdentifierCase.CDNKEY || pointer.getCdnNumber() != 0) {
//          builder.setRequiredProtocolVersion(Math.max(DataMessage.ProtocolVersion.CDN_SELECTOR_ATTACHMENTS_VALUE, builder.getRequiredProtocolVersion()));
//          break;
//        }
      }
    }

    if (message.getBody().isPresent()) {
      builder.body(message.getBody().get());
    }

    if (message.getGroupContext().isPresent()) {
      builder.groupV2(createGroupContent(message.getGroupContext().get()));
    }

    if (message.isEndSession()) {
      builder.flags(DataMessage.Flags.END_SESSION.getValue());
    }

    if (message.isExpirationUpdate()) {
      builder.flags(DataMessage.Flags.EXPIRATION_TIMER_UPDATE.getValue());
    }

    if (message.isProfileKeyUpdate()) {
      builder.flags(DataMessage.Flags.PROFILE_KEY_UPDATE.getValue());
    }

    if (message.getExpiresInSeconds() > 0) {
      builder.expireTimer(message.getExpiresInSeconds());
    }
    builder.expireTimerVersion(message.getExpireTimerVersion());

    if (message.getProfileKey().isPresent()) {
      builder.profileKey(ByteString.of(message.getProfileKey().get()));
    }

    if (message.getQuote().isPresent()) {
      DataMessage.Quote.Builder quoteBuilder = new DataMessage.Quote.Builder()
                                                                .id(message.getQuote().get().getId())
                                                                .text(message.getQuote().get().getText())
                                                                .authorAci(useStringId ? message.getQuote().get().getAuthor().toString() : null)
                                                                .authorAciBinary(useBinaryId ? message.getQuote().get().getAuthor().toByteString() : null)
                                                                .type(message.getQuote().get().getType().getProtoType());

      List<ZonaRosaServiceDataMessage.Mention> mentions = message.getQuote().get().getMentions();
      if (mentions != null && !mentions.isEmpty()) {
        List<BodyRange> bodyRanges = new ArrayList<>(quoteBuilder.bodyRanges);
        for (ZonaRosaServiceDataMessage.Mention mention : mentions) {
          if (useStringId) {
            bodyRanges.add(new BodyRange.Builder()
                               .start(mention.getStart())
                               .length(mention.getLength())
                               .mentionAci(mention.getServiceId().toString())
                               .build());
          } else {
            bodyRanges.add(new BodyRange.Builder()
                               .start(mention.getStart())
                               .length(mention.getLength())
                               .mentionAciBinary(mention.getServiceId().toByteString())
                               .build());
          }
        }
        quoteBuilder.bodyRanges(bodyRanges);

        builder.requiredProtocolVersion(Math.max(DataMessage.ProtocolVersion.MENTIONS.getValue(), builder.requiredProtocolVersion));
      }

      List<BodyRange> bodyRanges = message.getQuote().get().getBodyRanges();
      if (bodyRanges != null) {
        List<BodyRange> quoteBodyRanges = new ArrayList<>(quoteBuilder.bodyRanges);
        quoteBodyRanges.addAll(bodyRanges);
        quoteBuilder.bodyRanges(quoteBodyRanges);
      }

      List<ZonaRosaServiceDataMessage.Quote.QuotedAttachment> attachments = message.getQuote().get().getAttachments();
      if (attachments != null) {
        List<DataMessage.Quote.QuotedAttachment> quotedAttachments = new ArrayList<>(attachments.size());
        for (ZonaRosaServiceDataMessage.Quote.QuotedAttachment attachment : attachments) {
          DataMessage.Quote.QuotedAttachment.Builder quotedAttachment = new DataMessage.Quote.QuotedAttachment.Builder();

          quotedAttachment.contentType(attachment.getContentType());

          if (attachment.getFileName() != null) {
            quotedAttachment.fileName(attachment.getFileName());
          }

          if (attachment.getThumbnail() != null) {
            if (attachment.getThumbnail().isStream()) {
              quotedAttachment.thumbnail(createAttachmentPointer(attachment.getThumbnail().asStream()));
            } else {
              quotedAttachment.thumbnail(createAttachmentPointer(attachment.getThumbnail().asPointer()));
            }
          }

          quotedAttachments.add(quotedAttachment.build());
        }
        quoteBuilder.attachments(quotedAttachments);
      }

      builder.quote(quoteBuilder.build());
    }

    if (message.getSharedContacts().isPresent()) {
      builder.contact = createSharedContactContent(message.getSharedContacts().get());
    }

    if (message.getPreviews().isPresent()) {
      List<Preview> previews = new ArrayList<>(message.getPreviews().get().size());
      for (ZonaRosaServicePreview preview : message.getPreviews().get()) {
        previews.add(createPreview(preview));
      }
      builder.preview(previews);
    }

    if (message.getMentions().isPresent()) {
      List<BodyRange> bodyRanges = new ArrayList<>(builder.bodyRanges);
      for (ZonaRosaServiceDataMessage.Mention mention : message.getMentions().get()) {
        if (useStringId) {
          bodyRanges.add(new BodyRange.Builder()
                             .start(mention.getStart())
                             .length(mention.getLength())
                             .mentionAci(mention.getServiceId().toString())
                             .build());
        } else {
          bodyRanges.add(new BodyRange.Builder()
                             .start(mention.getStart())
                             .length(mention.getLength())
                             .mentionAciBinary(mention.getServiceId().toByteString())
                             .build());
        }
      }
      builder.bodyRanges(bodyRanges);
      builder.requiredProtocolVersion(Math.max(DataMessage.ProtocolVersion.MENTIONS.getValue(), builder.requiredProtocolVersion));
    }

    if (message.getSticker().isPresent()) {
      DataMessage.Sticker.Builder stickerBuilder = new DataMessage.Sticker.Builder();

      stickerBuilder.packId(ByteString.of(message.getSticker().get().getPackId()));
      stickerBuilder.packKey(ByteString.of(message.getSticker().get().getPackKey()));
      stickerBuilder.stickerId(message.getSticker().get().getStickerId());

      if (message.getSticker().get().getEmoji() != null) {
        stickerBuilder.emoji(message.getSticker().get().getEmoji());
      }

      if (message.getSticker().get().getAttachment().isStream()) {
        stickerBuilder.data_(createAttachmentPointer(message.getSticker().get().getAttachment().asStream()));
      } else {
        stickerBuilder.data_(createAttachmentPointer(message.getSticker().get().getAttachment().asPointer()));
      }

      builder.sticker(stickerBuilder.build());
    }

    if (message.isViewOnce()) {
      builder.isViewOnce(message.isViewOnce());
      builder.requiredProtocolVersion(Math.max(DataMessage.ProtocolVersion.VIEW_ONCE_VIDEO.getValue(), builder.requiredProtocolVersion));
    }

    if (message.getReaction().isPresent()) {
      DataMessage.Reaction.Builder reactionBuilder = new DataMessage.Reaction.Builder()
                                                                             .emoji(message.getReaction().get().getEmoji())
                                                                             .remove(message.getReaction().get().isRemove())
                                                                             .targetSentTimestamp(message.getReaction().get().getTargetSentTimestamp())
                                                                             .targetAuthorAci(useStringId ? message.getReaction().get().getTargetAuthor().toString() : null)
                                                                             .targetAuthorAciBinary(useBinaryId ? message.getReaction().get().getTargetAuthor().toByteString() : null);

      builder.reaction(reactionBuilder.build());
      builder.requiredProtocolVersion(Math.max(DataMessage.ProtocolVersion.REACTIONS.getValue(), builder.requiredProtocolVersion));
    }

    if (message.getRemoteDelete().isPresent()) {
      DataMessage.Delete delete = new DataMessage.Delete.Builder()
                                                        .targetSentTimestamp(message.getRemoteDelete().get().getTargetSentTimestamp())
                                                        .build();
      builder.delete(delete);
    }

    if (message.getGroupCallUpdate().isPresent()) {
      String eraId = message.getGroupCallUpdate().get().getEraId();
      if (eraId != null) {
        builder.groupCallUpdate(new DataMessage.GroupCallUpdate.Builder().eraId(eraId).build());
      } else {
        builder.groupCallUpdate(new DataMessage.GroupCallUpdate());
      }
    }

    if (message.getPayment().isPresent()) {
      ZonaRosaServiceDataMessage.Payment payment = message.getPayment().get();

      if (payment.getPaymentNotification().isPresent()) {
        ZonaRosaServiceDataMessage.PaymentNotification        paymentNotification = payment.getPaymentNotification().get();
        DataMessage.Payment.Notification.MobileCoin.Builder mobileCoinPayment   = new DataMessage.Payment.Notification.MobileCoin.Builder().receipt(ByteString.of(paymentNotification.getReceipt()));
        DataMessage.Payment.Notification.Builder            paymentBuilder      = new DataMessage.Payment.Notification.Builder()
                                                                                                                      .note(paymentNotification.getNote())
                                                                                                                      .mobileCoin(mobileCoinPayment.build());

        builder.payment(new DataMessage.Payment.Builder().notification(paymentBuilder.build()).build());
      } else if (payment.getPaymentActivation().isPresent()) {
        DataMessage.Payment.Activation.Builder activationBuilder = new DataMessage.Payment.Activation.Builder().type(payment.getPaymentActivation().get().getType());
        builder.payment(new DataMessage.Payment.Builder().activation(activationBuilder.build()).build());
      }
        builder.requiredProtocolVersion(Math.max(DataMessage.ProtocolVersion.PAYMENTS.getValue(), builder.requiredProtocolVersion));
    }

    if (message.getStoryContext().isPresent()) {
      ZonaRosaServiceDataMessage.StoryContext storyContext = message.getStoryContext().get();

      builder.storyContext(new DataMessage.StoryContext.Builder()
                                                       .authorAci(useStringId ? storyContext.getAuthorServiceId().toString() : null)
                                                       .authorAciBinary(useBinaryId ? storyContext.getAuthorServiceId().toByteString() : null)
                                                       .sentTimestamp(storyContext.getSentTimestamp())
                                                       .build());
    }

    if (message.getGiftBadge().isPresent()) {
      ZonaRosaServiceDataMessage.GiftBadge giftBadge = message.getGiftBadge().get();

      builder.giftBadge(new DataMessage.GiftBadge.Builder()
                                                 .receiptCredentialPresentation(ByteString.of(giftBadge.getReceiptCredentialPresentation().serialize()))
                                                 .build());
    }

    if (message.getBodyRanges().isPresent()) {
      List<BodyRange> bodyRanges = new ArrayList<>(builder.bodyRanges);
      bodyRanges.addAll(message.getBodyRanges().get());
      builder.bodyRanges(bodyRanges);
    }

    if (message.getPollCreate().isPresent()) {
      ZonaRosaServiceDataMessage.PollCreate pollCreate = message.getPollCreate().get();

      builder.pollCreate(new DataMessage.PollCreate.Builder()
                                                   .question(pollCreate.getQuestion())
                                                   .allowMultiple(pollCreate.getAllowMultiple())
                                                   .options(pollCreate.getOptions()).build());
      builder.requiredProtocolVersion(Math.max(DataMessage.ProtocolVersion.POLLS.getValue(), builder.requiredProtocolVersion));
    }

    if (message.getPollVote().isPresent()) {
      ZonaRosaServiceDataMessage.PollVote pollVote = message.getPollVote().get();
      builder.pollVote(new DataMessage.PollVote.Builder()
                                               .targetSentTimestamp(pollVote.getTargetSentTimestamp())
                                               .targetAuthorAciBinary(pollVote.getTargetAuthor().toByteString())
                                               .voteCount(pollVote.getVoteCount())
                                               .optionIndexes(pollVote.getOptionIndexes())
                                               .build());
    }

    if (message.getPollTerminate().isPresent()) {
      ZonaRosaServiceDataMessage.PollTerminate pollTerminate = message.getPollTerminate().get();
      builder.pollTerminate(new DataMessage.PollTerminate.Builder()
                                                         .targetSentTimestamp(pollTerminate.getTargetSentTimestamp())
                                                         .build());
    }

    if (message.getPinnedMessage().isPresent()) {
      ZonaRosaServiceDataMessage.PinnedMessage pinnedMessage = message.getPinnedMessage().get();
      if (Boolean.TRUE.equals(pinnedMessage.getForever())) {
        builder.pinMessage(new DataMessage.PinMessage.Builder()
                               .targetAuthorAciBinary(pinnedMessage.getTargetAuthor().toByteString())
                               .targetSentTimestamp(pinnedMessage.getTargetSentTimestamp())
                               .pinDurationForever(true)
                               .build());
      } else {
        builder.pinMessage(new DataMessage.PinMessage.Builder()
                               .targetAuthorAciBinary(pinnedMessage.getTargetAuthor().toByteString())
                               .targetSentTimestamp(pinnedMessage.getTargetSentTimestamp())
                               .pinDurationSeconds(pinnedMessage.getPinDurationInSeconds())
                               .build());
      }
    }

    if (message.getUnpinnedMessage().isPresent()) {
      ZonaRosaServiceDataMessage.UnpinnedMessage unpinnedMessage = message.getUnpinnedMessage().get();
      builder.unpinMessage(new DataMessage.UnpinMessage.Builder()
                               .targetAuthorAciBinary(unpinnedMessage.getTargetAuthor().toByteString())
                               .targetSentTimestamp(unpinnedMessage.getTargetSentTimestamp())
                               .build());
    }

    if (message.getAdminDelete().isPresent()) {
      ZonaRosaServiceDataMessage.AdminDelete adminDelete = message.getAdminDelete().get();
      builder.adminDelete(new DataMessage.AdminDelete.Builder()
                               .targetAuthorAciBinary(adminDelete.getTargetAuthor().toByteString())
                               .targetSentTimestamp(adminDelete.getTargetSentTimestamp())
                               .build());
    }

    builder.timestamp(message.getTimestamp());

    return builder;
  }

  private Preview createPreview(ZonaRosaServicePreview preview) throws IOException {
    Preview.Builder previewBuilder = new Preview.Builder()
                                                .title(preview.getTitle())
                                                .description(preview.getDescription())
                                                .date(preview.getDate())
                                                .url(preview.getUrl());

    if (preview.getImage().isPresent()) {
      if (preview.getImage().get().isStream()) {
        previewBuilder.image(createAttachmentPointer(preview.getImage().get().asStream()));
      } else {
        previewBuilder.image(createAttachmentPointer(preview.getImage().get().asPointer()));
      }
    }

    return previewBuilder.build();
  }

  private Content createCallContent(ZonaRosaServiceCallMessage callMessage) {
    Content.Builder     container = new Content.Builder();
    CallMessage.Builder builder   = new CallMessage.Builder();

    if (callMessage.getOfferMessage().isPresent()) {
      OfferMessage offer = callMessage.getOfferMessage().get();
      CallMessage.Offer.Builder offerBuilder = new CallMessage.Offer.Builder()
                                                                    .id(offer.getId())
                                                                    .type(offer.getType().getProtoType());

      if (offer.getOpaque() != null) {
        offerBuilder.opaque(ByteString.of(offer.getOpaque()));
      }

      builder.offer(offerBuilder.build());
    } else if (callMessage.getAnswerMessage().isPresent()) {
      AnswerMessage answer = callMessage.getAnswerMessage().get();
      CallMessage.Answer.Builder answerBuilder = new CallMessage.Answer.Builder()
                                                                       .id(answer.getId());

      if (answer.getOpaque() != null) {
        answerBuilder.opaque(ByteString.of(answer.getOpaque()));
      }

      builder.answer(answerBuilder.build());
    } else if (callMessage.getIceUpdateMessages().isPresent()) {
      List<IceUpdateMessage> updates = callMessage.getIceUpdateMessages().get();
      List<CallMessage.IceUpdate> iceUpdates = new ArrayList<>(updates.size());
      for (IceUpdateMessage update : updates) {
        CallMessage.IceUpdate.Builder iceBuilder = new CallMessage.IceUpdate.Builder()
                                                                            .id(update.getId());

        if (update.getOpaque() != null) {
          iceBuilder.opaque(ByteString.of(update.getOpaque()));
        }

        iceUpdates.add(iceBuilder.build());
      }
      builder.iceUpdate(iceUpdates);
    } else if (callMessage.getHangupMessage().isPresent()) {
      CallMessage.Hangup.Type    protoType        = callMessage.getHangupMessage().get().getType().getProtoType();
      CallMessage.Hangup.Builder builderForHangup = new CallMessage.Hangup.Builder()
                                                                          .type(protoType)
                                                                          .id(callMessage.getHangupMessage().get().getId());

      if (protoType != CallMessage.Hangup.Type.HANGUP_NORMAL) {
        builderForHangup.deviceId(callMessage.getHangupMessage().get().getDeviceId());
      }

      builder.hangup(builderForHangup.build());
    } else if (callMessage.getBusyMessage().isPresent()) {
      builder.busy(new CallMessage.Busy.Builder().id(callMessage.getBusyMessage().get().getId()).build());
    } else if (callMessage.getOpaqueMessage().isPresent()) {
      OpaqueMessage              opaqueMessage = callMessage.getOpaqueMessage().get();
      ByteString                 data          = ByteString.of(opaqueMessage.getOpaque());
      CallMessage.Opaque.Urgency urgency       = opaqueMessage.getUrgency().toProto();

      builder.opaque(new CallMessage.Opaque.Builder().data_(data).urgency(urgency).build());
    }

    if (callMessage.getDestinationDeviceId().isPresent()) {
      builder.destinationDeviceId(callMessage.getDestinationDeviceId().get());
    }

    container.callMessage(builder.build());
    return container.build();
  }

  private Content createMultiDeviceContactsContent(ZonaRosaServiceAttachmentStream contacts, boolean complete) throws IOException {
    Content.Builder     container = new Content.Builder();
    SyncMessage.Builder builder   = createSyncMessageBuilder();
    builder.contacts(new SyncMessage.Contacts.Builder()
                                             .blob(createAttachmentPointer(contacts))
                                             .complete(complete)
                                             .build());

    return container.syncMessage(builder.build()).build();
  }

  private Content createMultiDeviceSentTranscriptContent(SentTranscriptMessage transcript) throws IOException {
    ZonaRosaServiceAddress address = transcript.getDestination().get();
    Content              content = createMessageContent(transcript);
    SendMessageResult    result  = SendMessageResult.success(address, Collections.emptyList(), false, true, -1, Optional.ofNullable(content));


    return createMultiDeviceSentTranscriptContent(content,
                                                  Optional.of(address),
                                                  transcript.getTimestamp(),
                                                  Collections.singletonList(result),
                                                  transcript.isRecipientUpdate(),
                                                  transcript.getStoryMessageRecipients());
  }

  private Content createMultiDeviceSentTranscriptContent(Content content, Optional<ZonaRosaServiceAddress> recipient,
                                                         long timestamp, List<SendMessageResult> sendMessageResults,
                                                         boolean isRecipientUpdate,
                                                         Set<ZonaRosaServiceStoryMessageRecipient> storyMessageRecipients)
  {
    Content.Builder          container    = new Content.Builder();
    SyncMessage.Builder      syncMessage  = createSyncMessageBuilder();
    SyncMessage.Sent.Builder sentMessage  = new SyncMessage.Sent.Builder();
    DataMessage              dataMessage  = content != null && content.dataMessage != null ? content.dataMessage : null;
    StoryMessage             storyMessage = content != null && content.storyMessage != null ? content.storyMessage : null;
    EditMessage              editMessage  = content != null && content.editMessage != null ? content.editMessage : null;

    sentMessage.timestamp(timestamp);

    List<SyncMessage.Sent.UnidentifiedDeliveryStatus> unidentifiedDeliveryStatuses = new ArrayList<>(sendMessageResults.size());
    for (SendMessageResult result : sendMessageResults) {
      if (result.getSuccess() != null) {
        ByteString identity = null;

        if (result.getAddress().getServiceId() instanceof PNI) {
          IdentityKey identityKey = aciStore.getIdentity(result.getAddress().getServiceId().toProtocolAddress(ZonaRosaServiceAddress.DEFAULT_DEVICE_ID));
          if (identityKey != null) {
            identity = ByteString.of(identityKey.getPublicKey().serialize());
          } else {
            Log.w(TAG, "[" + timestamp + "] Could not find an identity for PNI when sending sync message! " + result.getAddress().getServiceId());
          }
        }

        unidentifiedDeliveryStatuses.add(new SyncMessage.Sent.UnidentifiedDeliveryStatus.Builder()
                                                                                        .destinationServiceId(useStringId ? result.getAddress().getServiceId().toString() : null)
                                                                                        .destinationServiceIdBinary(useBinaryId ? result.getAddress().getServiceId().toByteString() : null)
                                                                                        .unidentified(false)
                                                                                        .destinationPniIdentityKey(identity)
                                                                                        .build());
      }
    }
    sentMessage.unidentifiedStatus(unidentifiedDeliveryStatuses);

    if (recipient.isPresent()) {
      sentMessage.destinationServiceId(useStringId ? recipient.get().getServiceId().toString() : null);
      sentMessage.destinationServiceIdBinary(useBinaryId ? recipient.get().getServiceId().toByteString() : null);
      if (recipient.get().getNumber().isPresent()) {
        sentMessage.destinationE164(recipient.get().getNumber().get());
      }
    }

    if (dataMessage != null) {
      sentMessage.message(dataMessage);
      if (dataMessage.expireTimer != null && dataMessage.expireTimer > 0) {
        sentMessage.expirationStartTimestamp(System.currentTimeMillis());
      }

      if (dataMessage.isViewOnce != null && dataMessage.isViewOnce) {
        dataMessage = dataMessage.newBuilder().attachments(Collections.emptyList()).build();
        sentMessage.message(dataMessage);
      }
    }

    if (storyMessage != null) {
      sentMessage.storyMessage(storyMessage);
    }

    if (editMessage != null) {
      sentMessage.editMessage(editMessage);
    }

    Set<SyncMessage.Sent.StoryMessageRecipient> storyMessageRecipientsSet = storyMessageRecipients.stream()
                                                                                                  .map(this::createStoryMessageRecipient)
                                                                                                  .collect(Collectors.toSet());
    sentMessage.storyMessageRecipients(new ArrayList<>(storyMessageRecipientsSet));

    sentMessage.isRecipientUpdate(isRecipientUpdate);

    return container.syncMessage(syncMessage.sent(sentMessage.build()).build()).build();
  }
  
  private SyncMessage.Sent.StoryMessageRecipient createStoryMessageRecipient(ZonaRosaServiceStoryMessageRecipient storyMessageRecipient) {
    return new SyncMessage.Sent.StoryMessageRecipient.Builder()
                                                     .distributionListIds(storyMessageRecipient.getDistributionListIds())
                                                     .destinationServiceId(useStringId ? storyMessageRecipient.getZonaRosaServiceAddress().getIdentifier() : null)
                                                     .destinationServiceIdBinary(useBinaryId ? storyMessageRecipient.getZonaRosaServiceAddress().getServiceId().toByteString() : null)
                                                     .isAllowedToReply(storyMessageRecipient.isAllowedToReply())
                                                     .build();
  }

  private Content createMultiDeviceReadContent(List<ReadMessage> readMessages) {
    Content.Builder     container = new Content.Builder();
    SyncMessage.Builder builder   = createSyncMessageBuilder();

    builder.read(
        readMessages.stream()
                    .map(readMessage -> new SyncMessage.Read.Builder()
                                                            .timestamp(readMessage.getTimestamp())
                                                            .senderAci(useStringId ? readMessage.getSenderAci().toString() : null)
                                                            .senderAciBinary(useBinaryId ? readMessage.getSenderAci().toByteString() : null)
                                                            .build())
                    .collect(Collectors.toList())
    );

    return container.syncMessage(builder.build()).build();
  }

  private Content createMultiDeviceViewedContent(List<ViewedMessage> readMessages) {
    Content.Builder     container = new Content.Builder();
    SyncMessage.Builder builder   = createSyncMessageBuilder();

    builder.viewed(
        readMessages.stream()
                    .map(readMessage -> new SyncMessage.Viewed.Builder()
                                                              .timestamp(readMessage.getTimestamp())
                                                              .senderAci(useStringId ? readMessage.getSender().toString() : null)
                                                              .senderAciBinary(useBinaryId ? readMessage.getSender().toByteString() : null)
                                                              .build())
                    .collect(Collectors.toList())
    );

    return container.syncMessage(builder.build()).build();
  }

  private Content createMultiDeviceViewOnceOpenContent(ViewOnceOpenMessage readMessage) {
    Content.Builder                  container       = new Content.Builder();
    SyncMessage.Builder              builder         = createSyncMessageBuilder();

    builder.viewOnceOpen(new SyncMessage.ViewOnceOpen.Builder()
                                                     .timestamp(readMessage.getTimestamp())
                                                     .senderAci(useStringId ? readMessage.getSender().toString() : null)
                                                     .senderAciBinary(useBinaryId ? readMessage.getSender().toByteString() : null)
                                                     .build());

    return container.syncMessage(builder.build()).build();
  }

  private Content createMultiDeviceBlockedContent(BlockedListMessage blocked) {
    Content.Builder             container      = new Content.Builder();
    SyncMessage.Builder         syncMessage    = createSyncMessageBuilder();
    SyncMessage.Blocked.Builder blockedMessage = new SyncMessage.Blocked.Builder();

    blockedMessage.acis(useStringId ? blocked.individuals.stream().filter(a -> a.getAci() != null).map(a -> a.getAci().toString()).collect(Collectors.toList()) : Collections.emptyList());
    blockedMessage.acisBinary(useBinaryId ? blocked.individuals.stream().filter(a -> a.getAci() != null).map(a -> a.getAci().toByteString()).collect(Collectors.toList()) : Collections.emptyList());
    blockedMessage.numbers(blocked.individuals.stream().filter(a -> a.getE164() != null).map(a -> a.getE164()).collect(Collectors.toList()));
    blockedMessage.groupIds(blocked.groupIds.stream().map(ByteString::of).collect(Collectors.toList()));

    return container.syncMessage(syncMessage.blocked(blockedMessage.build()).build()).build();
  }

  private Content createMultiDeviceConfigurationContent(ConfigurationMessage configuration) {
    Content.Builder                   container            = new Content.Builder();
    SyncMessage.Builder               syncMessage          = createSyncMessageBuilder();
    SyncMessage.Configuration.Builder configurationMessage = new SyncMessage.Configuration.Builder();

    if (configuration.getReadReceipts().isPresent()) {
      configurationMessage.readReceipts(configuration.getReadReceipts().get());
    }

    if (configuration.getUnidentifiedDeliveryIndicators().isPresent()) {
      configurationMessage.unidentifiedDeliveryIndicators(configuration.getUnidentifiedDeliveryIndicators().get());
    }

    if (configuration.getTypingIndicators().isPresent()) {
      configurationMessage.typingIndicators(configuration.getTypingIndicators().get());
    }

    if (configuration.getLinkPreviews().isPresent()) {
      configurationMessage.linkPreviews(configuration.getLinkPreviews().get());
    }

    configurationMessage.provisioningVersion(ProvisioningVersion.CURRENT.getValue());

    return container.syncMessage(syncMessage.configuration(configurationMessage.build()).build()).build();
  }

  private Content createMultiDeviceStickerPackOperationContent(List<StickerPackOperationMessage> stickerPackOperations) {
    Content.Builder     container   = new Content.Builder();
    SyncMessage.Builder syncMessage = createSyncMessageBuilder();

    List<SyncMessage.StickerPackOperation> stickerPackOperationProtos = new ArrayList<>(stickerPackOperations.size());
    for (StickerPackOperationMessage stickerPackOperation : stickerPackOperations) {
      SyncMessage.StickerPackOperation.Builder builder = new SyncMessage.StickerPackOperation.Builder();

      if (stickerPackOperation.getPackId().isPresent()) {
        builder.packId(ByteString.of(stickerPackOperation.getPackId().get()));
      }

      if (stickerPackOperation.getPackKey().isPresent()) {
        builder.packKey(ByteString.of(stickerPackOperation.getPackKey().get()));
      }

      if (stickerPackOperation.getType().isPresent()) {
        switch (stickerPackOperation.getType().get()) {
          case INSTALL:
            builder.type(SyncMessage.StickerPackOperation.Type.INSTALL);
            break;
          case REMOVE:
            builder.type(SyncMessage.StickerPackOperation.Type.REMOVE);
            break;
        }
      }

      stickerPackOperationProtos.add(builder.build());
    }

    return container.syncMessage(syncMessage.stickerPackOperation(stickerPackOperationProtos).build()).build();
  }

  private Content createMultiDeviceFetchTypeContent(ZonaRosaServiceSyncMessage.FetchType fetchType) {
    Content.Builder                 container    = new Content.Builder();
    SyncMessage.Builder             syncMessage  = createSyncMessageBuilder();
    SyncMessage.FetchLatest.Builder fetchMessage = new SyncMessage.FetchLatest.Builder();

    switch (fetchType) {
      case LOCAL_PROFILE:
        fetchMessage.type(SyncMessage.FetchLatest.Type.LOCAL_PROFILE);
        break;
      case STORAGE_MANIFEST:
        fetchMessage.type(SyncMessage.FetchLatest.Type.STORAGE_MANIFEST);
        break;
      case SUBSCRIPTION_STATUS:
       fetchMessage.type(SyncMessage.FetchLatest.Type.SUBSCRIPTION_STATUS);
        break;
      default:
        Log.w(TAG, "Unknown fetch type!");
        break;
    }

    return container.syncMessage(syncMessage.fetchLatest(fetchMessage.build()).build()).build();
  }

  private Content createMultiDeviceMessageRequestResponseContent(MessageRequestResponseMessage message) {
    Content.Builder container = new Content.Builder();
    SyncMessage.Builder syncMessage = createSyncMessageBuilder();
    SyncMessage.MessageRequestResponse.Builder responseMessage = new SyncMessage.MessageRequestResponse.Builder();

    if (message.getGroupId().isPresent()) {
      responseMessage.groupId(ByteString.of(message.getGroupId().get()));
    }

    if (message.getPerson().isPresent()) {
      responseMessage.threadAci(useStringId ? message.getPerson().get().toString() : null);
      responseMessage.threadAciBinary(useBinaryId ? message.getPerson().get().toByteString() : null);
    }

    switch (message.getType()) {
      case ACCEPT:
        responseMessage.type(SyncMessage.MessageRequestResponse.Type.ACCEPT);
        break;
      case DELETE:
        responseMessage.type(SyncMessage.MessageRequestResponse.Type.DELETE);
        break;
      case BLOCK:
        responseMessage.type(SyncMessage.MessageRequestResponse.Type.BLOCK);
        break;
      case BLOCK_AND_DELETE:
        responseMessage.type(SyncMessage.MessageRequestResponse.Type.BLOCK_AND_DELETE);
        break;
      case SPAM:
        responseMessage.type(SyncMessage.MessageRequestResponse.Type.SPAM);
        break;
      case BLOCK_AND_SPAM:
        responseMessage.type(SyncMessage.MessageRequestResponse.Type.BLOCK_AND_SPAM);
        break;
      default:
        Log.w(TAG, "Unknown type!");
        responseMessage.type(SyncMessage.MessageRequestResponse.Type.UNKNOWN);
        break;
    }

    syncMessage.messageRequestResponse(responseMessage.build());

    return container.syncMessage(syncMessage.build()).build();
  }

  private Content createMultiDeviceOutgoingPaymentContent(OutgoingPaymentMessage message) {
    Content.Builder                     container      = new Content.Builder();
    SyncMessage.Builder                 syncMessage    = createSyncMessageBuilder();
    SyncMessage.OutgoingPayment.Builder paymentMessage = new SyncMessage.OutgoingPayment.Builder();

    if (message.getRecipient().isPresent()) {
      paymentMessage.recipientServiceId(message.getRecipient().get().toString());
    }

    if (message.getNote().isPresent()) {
      paymentMessage.note(message.getNote().get());
    }

    try {
      SyncMessage.OutgoingPayment.MobileCoin.Builder mobileCoinBuilder = new SyncMessage.OutgoingPayment.MobileCoin.Builder();

      if (message.getAddress().isPresent()) {
        mobileCoinBuilder.recipientAddress(ByteString.of(message.getAddress().get()));
      }
      mobileCoinBuilder.amountPicoMob(Uint64Util.bigIntegerToUInt64(message.getAmount().toPicoMobBigInteger()))
                       .feePicoMob(Uint64Util.bigIntegerToUInt64(message.getFee().toPicoMobBigInteger()))
                       .receipt(message.getReceipt())
                       .ledgerBlockTimestamp(message.getBlockTimestamp())
                       .ledgerBlockIndex(message.getBlockIndex())
                       .outputPublicKeys(message.getPublicKeys())
                       .spentKeyImages(message.getKeyImages());

      paymentMessage.mobileCoin(mobileCoinBuilder.build());
    } catch (Uint64RangeException e) {
      throw new AssertionError(e);
    }

    syncMessage.outgoingPayment(paymentMessage.build());

    return container.syncMessage(syncMessage.build()).build();
  }

  private Content createMultiDeviceSyncKeysContent(KeysMessage keysMessage) {
    Content.Builder          container   = new Content.Builder();
    SyncMessage.Builder      syncMessage = createSyncMessageBuilder();
    SyncMessage.Keys.Builder builder     = new SyncMessage.Keys.Builder();

    if (keysMessage.getMaster() != null) {
      builder.master(ByteString.of(keysMessage.getMaster().serialize()));
    }

    if (keysMessage.getAccountEntropyPool() != null) {
      builder.accountEntropyPool(keysMessage.getAccountEntropyPool().getValue());
    }

    if (keysMessage.getMediaRootBackupKey() != null) {
      builder.mediaRootBackupKey(ByteString.of(keysMessage.getMediaRootBackupKey().getValue()));
    }

    return container.syncMessage(syncMessage.keys(builder.build()).build()).build();
  }

  private Content createMultiDeviceVerifiedContent(VerifiedMessage verifiedMessage, byte[] nullMessage) {
    Content.Builder     container              = new Content.Builder();
    SyncMessage.Builder syncMessage            = createSyncMessageBuilder();
    Verified.Builder    verifiedMessageBuilder = new Verified.Builder();

    verifiedMessageBuilder.nullMessage(ByteString.of(nullMessage));
    verifiedMessageBuilder.identityKey(ByteString.of(verifiedMessage.getIdentityKey().serialize()));
    verifiedMessageBuilder.destinationAci(useStringId ? verifiedMessage.getDestination().getServiceId().toString() : null);
    verifiedMessageBuilder.destinationAciBinary(useBinaryId ? verifiedMessage.getDestination().getServiceId().toByteString() : null);


    switch (verifiedMessage.getVerified()) {
      case DEFAULT:    verifiedMessageBuilder.state(Verified.State.DEFAULT);    break;
      case VERIFIED:   verifiedMessageBuilder.state(Verified.State.VERIFIED);   break;
      case UNVERIFIED: verifiedMessageBuilder.state(Verified.State.UNVERIFIED); break;
      default:         throw new AssertionError("Unknown: " + verifiedMessage.getVerified());
    }

    syncMessage.verified(verifiedMessageBuilder.build());
    return container.syncMessage(syncMessage.build()).build();
  }

  private Content createRequestContent(SyncMessage.Request request) throws IOException {
    if (localDeviceId == ZonaRosaServiceAddress.DEFAULT_DEVICE_ID) {
      throw new IOException("Sync requests should only be sent from a linked device");
    }

    Content.Builder     container = new Content.Builder();
    SyncMessage.Builder builder   = createSyncMessageBuilder().request(request);

    return container.syncMessage(builder.build()).build();
  }

  private Content createCallEventContent(SyncMessage.CallEvent proto) {
    Content.Builder     container = new Content.Builder();
    SyncMessage.Builder builder   = createSyncMessageBuilder().callEvent(proto);

    return container.syncMessage(builder.build()).build();
  }

  private Content createCallLinkUpdateContent(SyncMessage.CallLinkUpdate proto) {
    Content.Builder     container = new Content.Builder();
    SyncMessage.Builder builder   = createSyncMessageBuilder().callLinkUpdate(proto);

    return container.syncMessage(builder.build()).build();
  }

  private Content createCallLogEventContent(SyncMessage.CallLogEvent proto) {
    Content.Builder     container = new Content.Builder();
    SyncMessage.Builder builder   = createSyncMessageBuilder().callLogEvent(proto);

    return container.syncMessage(builder.build()).build();
  }

  private Content createDeviceNameChangeContent(SyncMessage.DeviceNameChange proto) {
    Content.Builder     container = new Content.Builder();
    SyncMessage.Builder builder   = createSyncMessageBuilder().deviceNameChange(proto);

    return container.syncMessage(builder.build()).build();
  }

  private Content createAttachmentBackfillResponseContent(SyncMessage.AttachmentBackfillResponse proto) {
    Content.Builder     container = new Content.Builder();
    SyncMessage.Builder builder   = createSyncMessageBuilder().attachmentBackfillResponse(proto);

    return container.syncMessage(builder.build()).build();
  }

  private SyncMessage.Builder createSyncMessageBuilder() {
    byte[]       padding = Util.getRandomLengthSecretBytes(512);

    SyncMessage.Builder builder = new SyncMessage.Builder();
    builder.padding(ByteString.of(padding));

    return builder;
  }

  private static GroupContextV2 createGroupContent(ZonaRosaServiceGroupV2 group) {
    GroupContextV2.Builder builder = new GroupContextV2.Builder()
                                                       .masterKey(ByteString.of(group.getMasterKey().serialize()))
                                                       .revision(group.getRevision());

    byte[] signedGroupChange = group.getSignedGroupChange();
    if (signedGroupChange != null && signedGroupChange.length <= 2048) {
      builder.groupChange(ByteString.of(signedGroupChange));
    }

    return builder.build();
  }

  private List<DataMessage.Contact> createSharedContactContent(List<SharedContact> contacts) throws IOException {
    List<DataMessage.Contact> results = new LinkedList<>();

    for (SharedContact contact : contacts) {
      DataMessage.Contact.Name.Builder nameBuilder = new DataMessage.Contact.Name.Builder();

      if (contact.getName().getFamily().isPresent())   nameBuilder.familyName(contact.getName().getFamily().get());
      if (contact.getName().getGiven().isPresent())    nameBuilder.givenName(contact.getName().getGiven().get());
      if (contact.getName().getMiddle().isPresent())   nameBuilder.middleName(contact.getName().getMiddle().get());
      if (contact.getName().getPrefix().isPresent())   nameBuilder.prefix(contact.getName().getPrefix().get());
      if (contact.getName().getSuffix().isPresent())   nameBuilder.suffix(contact.getName().getSuffix().get());
      if (contact.getName().getNickname().isPresent()) nameBuilder.nickname(contact.getName().getNickname().get());

      DataMessage.Contact.Builder contactBuilder = new DataMessage.Contact.Builder().name(nameBuilder.build());

      if (contact.getAddress().isPresent()) {
        List<DataMessage.Contact.PostalAddress> postalAddresses = new ArrayList<>(contact.getAddress().get().size());
        for (SharedContact.PostalAddress address : contact.getAddress().get()) {
          DataMessage.Contact.PostalAddress.Builder addressBuilder = new DataMessage.Contact.PostalAddress.Builder();

          switch (address.getType()) {
            case HOME:   addressBuilder.type(DataMessage.Contact.PostalAddress.Type.HOME); break;
            case WORK:   addressBuilder.type(DataMessage.Contact.PostalAddress.Type.WORK); break;
            case CUSTOM: addressBuilder.type(DataMessage.Contact.PostalAddress.Type.CUSTOM); break;
            default:     throw new AssertionError("Unknown type: " + address.getType());
          }

          if (address.getCity().isPresent())         addressBuilder.city(address.getCity().get());
          if (address.getCountry().isPresent())      addressBuilder.country(address.getCountry().get());
          if (address.getLabel().isPresent())        addressBuilder.label(address.getLabel().get());
          if (address.getNeighborhood().isPresent()) addressBuilder.neighborhood(address.getNeighborhood().get());
          if (address.getPobox().isPresent())        addressBuilder.pobox(address.getPobox().get());
          if (address.getPostcode().isPresent())     addressBuilder.postcode(address.getPostcode().get());
          if (address.getRegion().isPresent())       addressBuilder.region(address.getRegion().get());
          if (address.getStreet().isPresent())       addressBuilder.street(address.getStreet().get());

          postalAddresses.add(addressBuilder.build());
        }
        contactBuilder.address(postalAddresses);
      }

      if (contact.getEmail().isPresent()) {
        List<DataMessage.Contact.Email> emails = new ArrayList<>(contact.getEmail().get().size());
        for (SharedContact.Email email : contact.getEmail().get()) {
          DataMessage.Contact.Email.Builder emailBuilder = new DataMessage.Contact.Email.Builder().value_(email.getValue());

          switch (email.getType()) {
            case HOME:   emailBuilder.type(DataMessage.Contact.Email.Type.HOME);   break;
            case WORK:   emailBuilder.type(DataMessage.Contact.Email.Type.WORK);   break;
            case MOBILE: emailBuilder.type(DataMessage.Contact.Email.Type.MOBILE); break;
            case CUSTOM: emailBuilder.type(DataMessage.Contact.Email.Type.CUSTOM); break;
            default:     throw new AssertionError("Unknown type: " + email.getType());
          }

          if (email.getLabel().isPresent()) emailBuilder.label(email.getLabel().get());

          emails.add(emailBuilder.build());
        }
        contactBuilder.email(emails);
      }

      if (contact.getPhone().isPresent()) {
        List<DataMessage.Contact.Phone> phones = new ArrayList<>(contact.getPhone().get().size());
        for (SharedContact.Phone phone : contact.getPhone().get()) {
          DataMessage.Contact.Phone.Builder phoneBuilder = new DataMessage.Contact.Phone.Builder().value_(phone.getValue());

          switch (phone.getType()) {
            case HOME:   phoneBuilder.type(DataMessage.Contact.Phone.Type.HOME);   break;
            case WORK:   phoneBuilder.type(DataMessage.Contact.Phone.Type.WORK);   break;
            case MOBILE: phoneBuilder.type(DataMessage.Contact.Phone.Type.MOBILE); break;
            case CUSTOM: phoneBuilder.type(DataMessage.Contact.Phone.Type.CUSTOM); break;
            default:     throw new AssertionError("Unknown type: " + phone.getType());
          }

          if (phone.getLabel().isPresent()) phoneBuilder.label(phone.getLabel().get());

          phones.add(phoneBuilder.build());
        }
        contactBuilder.number(phones);
      }

      if (contact.getAvatar().isPresent()) {
        AttachmentPointer pointer = contact.getAvatar().get().getAttachment().isStream() ? createAttachmentPointer(contact.getAvatar().get().getAttachment().asStream())
                                                                                         : createAttachmentPointer(contact.getAvatar().get().getAttachment().asPointer());
        contactBuilder.avatar(new DataMessage.Contact.Avatar.Builder()
                                                            .avatar(pointer)
                                                            .isProfile(contact.getAvatar().get().isProfile())
                                                            .build());
      }

      if (contact.getOrganization().isPresent()) {
        contactBuilder.organization(contact.getOrganization().get());
      }

      results.add(contactBuilder.build());
    }

    return results;
  }

  private ZonaRosaServiceSyncMessage createSelfSendSyncMessageForStory(ZonaRosaServiceStoryMessage message,
                                                                     long sentTimestamp,
                                                                     boolean isRecipientUpdate,
                                                                     Set<ZonaRosaServiceStoryMessageRecipient> manifest)
  {
    SentTranscriptMessage transcript = new SentTranscriptMessage(Optional.of(localAddress),
                                                                 sentTimestamp,
                                                                 Optional.empty(),
                                                                 0,
                                                                 Collections.singletonMap(localAddress.getServiceId(), false),
                                                                 isRecipientUpdate,
                                                                 Optional.of(message),
                                                                 manifest,
                                                                 Optional.empty());

    return ZonaRosaServiceSyncMessage.forSentTranscript(transcript);
  }

  private ZonaRosaServiceSyncMessage createSelfSendSyncMessage(ZonaRosaServiceDataMessage message) {
    SentTranscriptMessage transcript = new SentTranscriptMessage(Optional.of(localAddress),
                                                                 message.getTimestamp(),
                                                                 Optional.of(message),
                                                                 message.getExpiresInSeconds(),
                                                                 Collections.singletonMap(localAddress.getServiceId(), false),
                                                                 false,
                                                                 Optional.empty(),
                                                                 Collections.emptySet(),
                                                                 Optional.empty());
    return ZonaRosaServiceSyncMessage.forSentTranscript(transcript);
  }

  private ZonaRosaServiceSyncMessage createSelfSendSyncEditMessage(ZonaRosaServiceEditMessage message) {
    SentTranscriptMessage transcript = new SentTranscriptMessage(Optional.of(localAddress),
                                                                 message.getDataMessage().getTimestamp(),
                                                                 Optional.empty(),
                                                                 message.getDataMessage().getExpiresInSeconds(),
                                                                 Collections.singletonMap(localAddress.getServiceId(), false),
                                                                 false,
                                                                 Optional.empty(),
                                                                 Collections.emptySet(),
                                                                 Optional.of(message));
    return ZonaRosaServiceSyncMessage.forSentTranscript(transcript);
  }

  private SendMessageResult sendMessage(ZonaRosaServiceAddress recipient,
                                        @Nullable SealedSenderAccess sealedSenderAccess,
                                        long timestamp,
                                        EnvelopeContent content,
                                        boolean online,
                                        CancelationZonaRosa cancelationZonaRosa,
                                        SendEvents sendEvents,
                                        boolean urgent,
                                        boolean story)
      throws UntrustedIdentityException, IOException
  {
    enforceMaxEnvelopeContentSize(content);

    long startTime = System.currentTimeMillis();

    for (int i = 0; i < RETRY_COUNT; i++) {
      if (cancelationZonaRosa != null && cancelationZonaRosa.isCanceled()) {
        return SendMessageResult.canceledFailure(recipient);
      }

      try {
        OutgoingPushMessageList messages = getEncryptedMessages(recipient,
                                                                sealedSenderAccess,
                                                                timestamp,
                                                                content,
                                                                online,
                                                                urgent,
                                                                story);
        if (i == 0 && sendEvents != null) {
          sendEvents.onMessageEncrypted();
        }

        if (content.getContent().isPresent() && content.getContent().get().syncMessage != null && content.getContent().get().syncMessage.sent != null) {
          Log.d(TAG, "[sendMessage][" + timestamp + "] Sending a sent sync message to devices: " + messages.getDevices());
        } else if (content.getContent().isPresent() && content.getContent().get().senderKeyDistributionMessage != null) {
          Log.d(TAG, "[sendMessage][" + timestamp + "] Sending a SKDM to " + messages.getDestination() + " for devices: " + messages.getDevices() + (content.getContent().get().dataMessage != null ? " (it's piggy-backing on a DataMessage)" : ""));
        }

        if (cancelationZonaRosa != null && cancelationZonaRosa.isCanceled()) {
          return SendMessageResult.canceledFailure(recipient);
        }

        try {
          SendMessageResponse response = NetworkResultUtil.toMessageSendLegacy(messages.getDestination(), messageApi.sendMessage(messages, sealedSenderAccess, story));
          return SendMessageResult.success(recipient, messages.getDevices(), response.sentUnidentified(), response.getNeedsSync() || aciStore.isMultiDevice(), System.currentTimeMillis() - startTime, content.getContent());
        } catch (AuthorizationFailedException |
                 UnregisteredUserException |
                 MismatchedDevicesException |
                 StaleDevicesException |
                 ProofRequiredException |
                 ServerRejectedException |
                 RateLimitException e) {
          // Non-technical failures shouldn't be retried with socket
          throw e;
        } catch (WebSocketUnavailableException e) {
          String pipe = sealedSenderAccess == null ? "Pipe" : "Unidentified pipe";
          if (useRestFallback.getAsBoolean()) {
            Log.i(TAG, "[sendMessage][" + timestamp + "] " + pipe + " unavailable, falling back... (" + e.getClass().getSimpleName() + ": " + e.getMessage() + ")");
          } else {
            Log.i(TAG, "[sendMessage][" + timestamp + "] " + pipe + " unavailable (" + e.getClass().getSimpleName() + ": " + e.getMessage() + ")");
            throw e;
          }
        } catch (IOException e) {
          String pipe = sealedSenderAccess == null ? "Pipe" : "Unidentified pipe";
          Throwable cause = e;
          if (e.getCause() != null) {
            cause = e.getCause();
          }

          if (useRestFallback.getAsBoolean()) {
            Log.w(TAG, "[sendMessage][" + timestamp + "] " + pipe + " failed, falling back... (" + cause.getClass().getSimpleName() + ": " + cause.getMessage() + ")");
          } else {
            Log.w(TAG, "[sendMessage][" + timestamp + "] " + pipe + " failed (" + cause.getClass().getSimpleName() + ": " + cause.getMessage() + ")");
            throw (cause instanceof IOException) ? (IOException) cause : e;
          }
        }

        if (cancelationZonaRosa != null && cancelationZonaRosa.isCanceled()) {
          return SendMessageResult.canceledFailure(recipient);
        }

        SendMessageResponse response = socket.sendMessage(messages, sealedSenderAccess, story);

        return SendMessageResult.success(recipient, messages.getDevices(), response.sentUnidentified(), response.getNeedsSync() || aciStore.isMultiDevice(), System.currentTimeMillis() - startTime, content.getContent());

      } catch (InvalidKeyException ike) {
        Log.w(TAG, ike);
        if (sealedSenderAccess != null) {
          sealedSenderAccess = sealedSenderAccess.switchToFallback();
        }
      } catch (AuthorizationFailedException afe) {
        if (sealedSenderAccess != null) {
          Log.w(TAG, "Got an AuthorizationFailedException when trying to send using sealed sender. Falling back.");
          sealedSenderAccess = sealedSenderAccess.switchToFallback();
        } else {
          Log.w(TAG, "Got an AuthorizationFailedException without using sealed sender!", afe);
          throw afe;
        }
      } catch (MismatchedDevicesException mde) {
        Log.w(TAG, "[sendMessage][" + timestamp + "] Handling mismatched devices. (" + mde.getMessage() + ")");
        handleMismatchedDevices(recipient, mde.getMismatchedDevices());
      } catch (StaleDevicesException ste) {
        Log.w(TAG, "[sendMessage][" + timestamp + "] Handling stale devices. (" + ste.getMessage() + ")");
        handleStaleDevices(recipient, ste.getStaleDevices());
      }
    }

    throw new IOException("Failed to resolve conflicts after " + RETRY_COUNT + " attempts!");
  }

  /**
   * Send a message to multiple recipients.
   *
   * @return An unordered list of a {@link SendMessageResult} for each send.
   * @throws IOException - Unknown failure or a failure not representable by an unsuccessful {@code SendMessageResult}.
   */
  private List<SendMessageResult> sendMessage(List<ZonaRosaServiceAddress> recipients,
                                              List<SealedSenderAccess> sealedSenderAccesses,
                                              long timestamp,
                                              EnvelopeContent content,
                                              boolean online,
                                              PartialSendCompleteListener partialListener,
                                              CancelationZonaRosa cancelationZonaRosa,
                                              @Nullable SendEvents sendEvents,
                                              boolean urgent,
                                              boolean story)
      throws IOException
  {
    Log.d(TAG, "[" + timestamp + "] Sending to " + recipients.size() + " recipients.");
    enforceMaxEnvelopeContentSize(content);

    long startTime = System.currentTimeMillis();

    eagerlyFetchMissingPreKeys(recipients, sealedSenderAccesses, story);

    List<Observable<SendMessageResult>> singleResults              = new LinkedList<>();
    Iterator<ZonaRosaServiceAddress>      recipientIterator          = recipients.iterator();
    Iterator<SealedSenderAccess>        sealedSenderAccessIterator = sealedSenderAccesses.iterator();

    while (recipientIterator.hasNext()) {
      ZonaRosaServiceAddress recipient          = recipientIterator.next();
      SealedSenderAccess   sealedSenderAccess = sealedSenderAccessIterator.next();

      singleResults.add(sendMessageRx(recipient, sealedSenderAccess, timestamp, content, online, cancelationZonaRosa, sendEvents, urgent, story, 0).toObservable());
    }

    List<SendMessageResult> results;
    try {
      results = Observable.mergeDelayError(singleResults, Integer.MAX_VALUE, 1)
                          .observeOn(scheduler, true)
                          .scan(new ArrayList<SendMessageResult>(singleResults.size()), (state, result) -> {
                            state.add(result);
                            if (partialListener != null) {
                              partialListener.onPartialSendComplete(result);
                            }
                            return state;
                          })
                          .lastOrError()
                          .blockingGet();
    } catch (RuntimeException e) {
      Throwable cause = e instanceof CompositeException ? ((CompositeException) e).getExceptions().get(0)
                                                        : e.getCause();

      if (cause instanceof IOException) {
        throw (IOException) cause;
      } else if (cause instanceof InterruptedException) {
        List<SendMessageResult> canceledResults = new ArrayList<>(recipients.size());
        for (ZonaRosaServiceAddress recipient : recipients) {
          canceledResults.add(SendMessageResult.canceledFailure(recipient));
        }
        return canceledResults;
      } else {
        throw e;
      }
    }

    double sendsForAverage = 0;
    for (SendMessageResult result : results) {
      if (result.getSuccess() != null && result.getSuccess().getDuration() != -1) {
        sendsForAverage++;
      }
    }

    double average = 0;
    if (sendsForAverage > 0) {
      for (SendMessageResult result : results) {
        if (result.getSuccess() != null && result.getSuccess().getDuration() != -1) {
          average += result.getSuccess().getDuration() / sendsForAverage;
        }
      }
    }

    Log.d(TAG, "[" + timestamp + "] Completed send to " + recipients.size() + " recipients in " + (System.currentTimeMillis() - startTime) + " ms, with an average time of " + Math.round(average) + " ms per send via Rx.");
    return results;
  }

  /**
   * Sends a message over the appropriate websocket, falls back to REST when unavailable, and emits a {@link SendMessageResult} for most business
   * logic error cases.
   * <p>
   * Uses a "feature" or Rx where if no {@link Single#subscribeOn(Scheduler)} operator is used, the subscribing thread is used to perform the
   * initial work. This allows the calling thread to do the starting of the send work (encryption and putting it on the wire) and can be called
   * multiple times in a loop, but allow the network transit/processing/error retry logic to run on a background thread.
   * <p>
   * Processing happens on the background thread via an {@link Single#observeOn(Scheduler)} call after the encrypt and send. Error
   * handling operators are added after the observe so they will also run on a background thread. Retry logic during error handling
   * is a recursive call, so error handling thread becomes the method "calling and subscribing" thread so all retries will perform the
   * encryption/send/processing on that background thread.
   *
   * @return A single that wraps success and business failures as a {@link SendMessageResult} but will still emit unhandled/unrecoverable
   * errors via {@code onError}
   */
  private Single<SendMessageResult> sendMessageRx(ZonaRosaServiceAddress recipient,
                                                  @Nullable SealedSenderAccess sealedSenderAccess,
                                                  long timestamp,
                                                  EnvelopeContent content,
                                                  boolean online,
                                                  CancelationZonaRosa cancelationZonaRosa,
                                                  @Nullable SendEvents sendEvents,
                                                  boolean urgent,
                                                  boolean story,
                                                  int retryCount)
  {
    long startTime = System.currentTimeMillis();
    enforceMaxEnvelopeContentSize(content);

    Single<OutgoingPushMessageList> messagesSingle = Single.fromCallable(() -> {
      OutgoingPushMessageList messages = getEncryptedMessages(recipient, sealedSenderAccess, timestamp, content, online, urgent, story);

      if (retryCount == 0 && sendEvents != null) {
        sendEvents.onMessageEncrypted();
      }

      if (content.getContent().isPresent() && content.getContent().get().syncMessage != null && content.getContent().get().syncMessage.sent != null) {
        Log.d(TAG, "[sendMessage][" + timestamp + "] Sending a sent sync message to devices: " + messages.getDevices() + " via Rx");
      } else if (content.getContent().isPresent() && content.getContent().get().senderKeyDistributionMessage != null) {
        Log.d(TAG, "[sendMessage][" + timestamp + "] Sending a SKDM to " + messages.getDestination() + " for devices: " + messages.getDevices() + (content.getContent().get().dataMessage != null ? " (it's piggy-backing on a DataMessage) via Rx" : " via Rx"));
      }

      return messages;
    });

    Single<SendMessageResult> sendWithFallback = messagesSingle
        .flatMap(messages -> {
          if (cancelationZonaRosa != null && cancelationZonaRosa.isCanceled()) {
            return Single.just(new kotlin.Pair<>(messages, new NetworkResult.NetworkError<SendMessageResponse>(new IOException("Canceled"))));
          }

          return Single.fromCallable(() -> messageApi.sendMessage(messages, sealedSenderAccess, story))
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .onErrorReturn(NetworkResult.ApplicationError::new)
                .map(r -> new kotlin.Pair<>(messages, r));
        })
        .observeOn(scheduler)
        .flatMap(pair -> {
          final OutgoingPushMessageList            messages      = pair.getFirst();
          final NetworkResult<SendMessageResponse> networkResult = pair.getSecond();

          try {
            SendMessageResponse response = NetworkResultUtil.toMessageSendLegacy(messages.getDestination(), networkResult);
            SendMessageResult   result   = SendMessageResult.success(
                recipient,
                messages.getDevices(),
                response.sentUnidentified(),
                response.getNeedsSync() || aciStore.isMultiDevice(),
                System.currentTimeMillis() - startTime,
                content.getContent()
            );
            return Single.just(result);
          } catch (IOException throwable) {
            if (cancelationZonaRosa != null && cancelationZonaRosa.isCanceled()) {
              return Single.just(SendMessageResult.canceledFailure(recipient));
            }

            if (throwable instanceof AuthorizationFailedException ||
                throwable instanceof UnregisteredUserException ||
                throwable instanceof MismatchedDevicesException ||
                throwable instanceof StaleDevicesException ||
                throwable instanceof ProofRequiredException ||
                throwable instanceof ServerRejectedException ||
                throwable instanceof RateLimitException)
            {
              // Non-technical failures shouldn't be retried with socket
              return Single.error(throwable);
            } else if (throwable instanceof WebSocketUnavailableException) {
              if (useRestFallback.getAsBoolean()) {
                Log.i(TAG, "[sendMessage][" + timestamp + "] " + (sealedSenderAccess != null ? "Unidentified " : "") + "pipe unavailable, falling back... (" + throwable.getClass().getSimpleName() + ": " + throwable.getMessage() + ")");
              } else {
                Log.i(TAG, "[sendMessage][" + timestamp + "] " + (sealedSenderAccess != null ? "Unidentified " : "") + "pipe unavailable (" + throwable.getClass().getSimpleName() + ": " + throwable.getMessage() + ")");
                return Single.error(throwable);
              }
            } else {
              Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
              if (useRestFallback.getAsBoolean()) {
                Log.w(TAG, "[sendMessage][" + timestamp + "] " + (sealedSenderAccess != null ? "Unidentified " : "") + "pipe failed, falling back... (" + cause.getClass().getSimpleName() + ": " + cause.getMessage() + ")");
              } else {
                Log.w(TAG, "[sendMessage][" + timestamp + "] " + (sealedSenderAccess != null ? "Unidentified " : "") + "pipe failed (" + cause.getClass().getSimpleName() + ": " + cause.getMessage() + ")");
                return Single.error((cause instanceof IOException) ? cause : throwable);
              }
            }

            return Single.fromCallable(() -> {
              SendMessageResponse response = socket.sendMessage(messages, sealedSenderAccess, story);
              return SendMessageResult.success(
                  recipient,
                  messages.getDevices(),
                  response.sentUnidentified(),
                  response.getNeedsSync() || aciStore.isMultiDevice(),
                  System.currentTimeMillis() - startTime,
                  content.getContent()
              );
            }).subscribeOn(scheduler);
          }
        });

    return sendWithFallback.onErrorResumeNext(t -> {
      if (cancelationZonaRosa != null && cancelationZonaRosa.isCanceled()) {
        return Single.just(SendMessageResult.canceledFailure(recipient));
      }

      if (retryCount >= RETRY_COUNT) {
        return Single.error(t);
      }

      if (t instanceof InvalidKeyException) {
        Log.w(TAG, t);
        return sendMessageRx(
            recipient,
            SealedSenderAccess.NONE,
            timestamp,
            content,
            online,
            cancelationZonaRosa,
            sendEvents,
            urgent,
            story,
            retryCount + 1
        );
      } else if (t instanceof AuthorizationFailedException) {
        if (sealedSenderAccess != null) {
          Log.w(TAG, "Got an AuthorizationFailedException when trying to send using sealed sender. Falling back.");
          return sendMessageRx(
              recipient,
              sealedSenderAccess.switchToFallback(),
              timestamp,
              content,
              online,
              cancelationZonaRosa,
              sendEvents,
              urgent,
              story,
              retryCount + 1
          );
        } else {
          Log.w(TAG, "Got an AuthorizationFailedException without using sealed sender!", t);
          return Single.error(t);
        }
      } else if (t instanceof MismatchedDevicesException) {
        MismatchedDevicesException mde = (MismatchedDevicesException) t;
        Log.w(TAG, "[sendMessage][" + timestamp + "] Handling mismatched devices. (" + mde.getMessage() + ")");

        return Single.fromCallable(() -> {
                       handleMismatchedDevices(recipient, mde.getMismatchedDevices());
                       return Unit.INSTANCE;
                     })
                     .flatMap(unused -> sendMessageRx(
                         recipient,
                         sealedSenderAccess,
                         timestamp,
                         content,
                         online,
                         cancelationZonaRosa,
                         sendEvents,
                         urgent,
                         story,
                         retryCount + 1)
                     );
      } else if (t instanceof StaleDevicesException) {
        StaleDevicesException ste = (StaleDevicesException) t;
        Log.w(TAG, "[sendMessage][" + timestamp + "] Handling stale devices. (" + ste.getMessage() + ")");

        return Single.fromCallable(() -> {
                       handleStaleDevices(recipient, ste.getStaleDevices());
                       return Unit.INSTANCE;
                     })
                     .flatMap(unused -> sendMessageRx(
                         recipient,
                         sealedSenderAccess,
                         timestamp,
                         content,
                         online,
                         cancelationZonaRosa,
                         sendEvents,
                         urgent,
                         story,
                         retryCount + 1)
                     );
      }

      return Single.error(t);
    }).onErrorResumeNext(t -> {
      try {
        SendMessageResult result = mapSendErrorToSendResult(t, timestamp, recipient);
        return Single.just(result);
      } catch (IOException e) {
        return Single.error(e);
      }
    });
  }

  /**
   * Converts common exceptions thrown during message sending to the appropriate {@link SendMessageResult}.
   * <p>
   * Exceptions that cannot be mapped will be rethrown as wrapped {@link IOException}s.
   */
  public static @Nonnull SendMessageResult mapSendErrorToSendResult(@Nonnull Throwable t, long timestamp, @Nonnull ZonaRosaServiceAddress recipient) throws IOException {
    if (t instanceof UntrustedIdentityException) {
      Log.w(TAG, "[" + timestamp + "] Hit identity mismatch: " + recipient.getIdentifier(), t);
      return SendMessageResult.identityFailure(recipient, ((UntrustedIdentityException) t).getIdentityKey());
    } else if (t instanceof UnregisteredUserException) {
      Log.w(TAG, "[" + timestamp + "] Hit unregistered user: " + recipient.getIdentifier());
      return SendMessageResult.unregisteredFailure(recipient);
    } else if (t instanceof PushNetworkException) {
      Log.w(TAG, "[" + timestamp + "] Hit network failure: " + recipient.getIdentifier(), t);
      return SendMessageResult.networkFailure(recipient);
    } else if (t instanceof ServerRejectedException) {
      Log.w(TAG, "[" + timestamp + "] Hit server rejection: " + recipient.getIdentifier(), t);
      throw (ServerRejectedException) t;
    } else if (t instanceof ProofRequiredException) {
      Log.w(TAG, "[" + timestamp + "] Hit proof required: " + recipient.getIdentifier(), t);
      return SendMessageResult.proofRequiredFailure(recipient, (ProofRequiredException) t);
    } else if (t instanceof RateLimitException) {
      Log.w(TAG, "[" + timestamp + "] Hit rate limit: " + recipient.getIdentifier(), t);
      return SendMessageResult.rateLimitFailure(recipient, (RateLimitException) t);
    } else if (t instanceof InvalidPreKeyException) {
      Log.w(TAG, "[" + timestamp + "] Hit invalid prekey: " + recipient.getIdentifier(), t);
      return SendMessageResult.invalidPreKeyFailure(recipient);
    } else {
      Log.w(TAG, "[" + timestamp + "] Hit unknown exception: " + recipient.getIdentifier(), t);
      throw new IOException(t);
    }
  }

  /**
   * Will send a message using sender keys to all of the specified recipients. It is assumed that
   * all of the recipients have UUIDs.
   *
   * This method will handle sending out SenderKeyDistributionMessages as necessary.
   */
  private List<SendMessageResult> sendGroupMessage(DistributionId distributionId,
                                                   List<ZonaRosaServiceAddress> recipients,
                                                   List<UnidentifiedAccess> unidentifiedAccess,
                                                   @Nullable GroupSendEndorsements groupSendEndorsements,
                                                   long timestamp,
                                                   Content content,
                                                   ContentHint contentHint,
                                                   Optional<byte[]> groupId,
                                                   boolean online,
                                                   SenderKeyGroupEvents sendEvents,
                                                   boolean urgent,
                                                   boolean story)
      throws IOException, UntrustedIdentityException, NoSessionException, InvalidKeyException, InvalidRegistrationIdException
  {
    if (recipients.isEmpty()) {
      Log.w(TAG, "[sendGroupMessage][" + timestamp + "] Empty recipient list!");
      return Collections.emptyList();
    }

    Preconditions.checkArgument(groupSendEndorsements != null || story, "[" + timestamp + "] GSE is null and not sending a story");
    Preconditions.checkArgument(recipients.size() == unidentifiedAccess.size(), "[" + timestamp + "] Unidentified access mismatch!");

    Map<ServiceId, UnidentifiedAccess> accessBySid     = new HashMap<>();
    Iterator<ZonaRosaServiceAddress>     addressIterator = recipients.iterator();
    Iterator<UnidentifiedAccess>       accessIterator  = unidentifiedAccess.iterator();

    while (addressIterator.hasNext()) {
      accessBySid.put(addressIterator.next().getServiceId(), accessIterator.next());
    }

    SenderCertificate  senderCertificate  = unidentifiedAccess.stream().filter(Objects::nonNull).findFirst().map(UnidentifiedAccess::getUnidentifiedCertificate).orElse(null);
    SealedSenderAccess sealedSenderAccess = SealedSenderAccess.forGroupSend(senderCertificate, groupSendEndorsements, story);

    for (int i = 0; i < RETRY_COUNT; i++) {
            GroupTargetInfo targetInfo         = buildGroupTargetInfo(recipients);
      final GroupTargetInfo targetInfoSnapshot = targetInfo;

      Set<ZonaRosaProtocolAddress> sharedWith            = aciStore.getSenderKeySharedWith(distributionId);
      List<ZonaRosaServiceAddress> needsSenderKeyTargets = targetInfo.destinations.stream()
                                                                                .filter(a -> !sharedWith.contains(a) || targetInfoSnapshot.sessions.get(a) == null)
                                                                                .map(a -> ServiceId.parseOrThrow(a.getName()))
                                                                                .distinct()
                                                                                .map(ZonaRosaServiceAddress::new)
                                                                                .collect(Collectors.toList());
      if (needsSenderKeyTargets.size() > 0) {
        Log.i(TAG, "[sendGroupMessage][" + timestamp + "] Need to send the distribution message to " + needsSenderKeyTargets.size() + " addresses.");
        SenderKeyDistributionMessage senderKeyDistributionMessage = getOrCreateNewGroupSession(distributionId);
        List<UnidentifiedAccess>     needsSenderKeyAccesses       = needsSenderKeyTargets.stream()
                                                                                         .map(r -> accessBySid.get(r.getServiceId()))
                                                                                         .collect(Collectors.toList());

        List<GroupSendFullToken> needsSenderKeyGroupSendTokens      = groupSendEndorsements != null ? groupSendEndorsements.forIndividuals(needsSenderKeyTargets) : null;
        List<SealedSenderAccess> needsSenderKeySealedSenderAccesses = SealedSenderAccess.forFanOutGroupSend(needsSenderKeyGroupSendTokens, sealedSenderAccess.getSenderCertificate(), needsSenderKeyAccesses);

        List<SendMessageResult> results = sendSenderKeyDistributionMessage(distributionId,
                                                                           needsSenderKeyTargets,
                                                                           needsSenderKeySealedSenderAccesses,
                                                                           senderKeyDistributionMessage,
                                                                           groupId,
                                                                           urgent,
                                                                           story && groupId.isEmpty()); // We don't want to flag SKDM's as stories for group stories, since we reuse distributionIds for normal group messages

        List<ZonaRosaServiceAddress> successes = results.stream()
                                                      .filter(SendMessageResult::isSuccess)
                                                      .map(SendMessageResult::getAddress)
                                                      .collect(Collectors.toList());

        Set<String>                successSids      = successes.stream().map(a -> a.getServiceId().toString()).collect(Collectors.toSet());
        Set<ZonaRosaProtocolAddress> successAddresses = targetInfo.destinations.stream().filter(a -> successSids.contains(a.getName())).collect(Collectors.toSet());

        aciStore.markSenderKeySharedWith(distributionId, successAddresses);

        Log.i(TAG, "[sendGroupMessage][" + timestamp + "] Successfully sent sender keys to " + successes.size() + "/" + needsSenderKeyTargets.size() + " recipients.");

        int failureCount = results.size() - successes.size();
        if (failureCount > 0) {
          Log.w(TAG, "[sendGroupMessage][" + timestamp + "] Failed to send sender keys to " + failureCount + " recipients. Sending back failed results now.");

          List<SendMessageResult> trueFailures = results.stream()
                                                        .filter(r -> !r.isSuccess())
                                                        .collect(Collectors.toList());

          Set<ServiceId> failedAddresses = trueFailures.stream()
                                                       .map(result -> result.getAddress().getServiceId())
                                                       .collect(Collectors.toSet());

          List<SendMessageResult> fakeNetworkFailures = recipients.stream()
                                                                  .filter(r -> !failedAddresses.contains(r.getServiceId()))
                                                                  .map(SendMessageResult::networkFailure)
                                                                  .collect(Collectors.toList());

          List<SendMessageResult> modifiedResults = new LinkedList<>();
          modifiedResults.addAll(trueFailures);
          modifiedResults.addAll(fakeNetworkFailures);

          return modifiedResults;
        } else {
          targetInfo = buildGroupTargetInfo(recipients);
        }
      }

      sendEvents.onSenderKeyShared();

      ZonaRosaServiceCipher cipher = new ZonaRosaServiceCipher(localAddress, localDeviceId, aciStore, sessionLock, null);

      byte[] ciphertext;
      try {
        ciphertext = cipher.encryptForGroup(distributionId, targetInfo.destinations, targetInfo.sessions, sealedSenderAccess.getSenderCertificate(), content.encode(), contentHint, groupId);
      } catch (io.zonarosa.libzonarosa.protocol.UntrustedIdentityException e) {
        throw new UntrustedIdentityException("Untrusted during group encrypt", e.getName(), e.getUntrustedIdentity());
      }

      sendEvents.onMessageEncrypted();

      try {
        try {

          SendGroupMessageResponse response = NetworkResultUtil.toGroupMessageSendLegacy(messageApi.sendGroupMessage(ciphertext, sealedSenderAccess, timestamp, online, urgent, story));
          return transformGroupResponseToMessageResults(targetInfo.devices, response, content);
        } catch (InvalidUnidentifiedAccessHeaderException |
                 NotFoundException |
                 GroupMismatchedDevicesException |
                 GroupStaleDevicesException |
                 ServerRejectedException |
                 RateLimitException e) {
          // Non-technical failures shouldn't be retried with socket
          throw e;
        } catch (WebSocketUnavailableException e) {
          if (useRestFallback.getAsBoolean()) {
            Log.i(TAG, "[sendGroupMessage][" + timestamp + "] Pipe unavailable, falling back... (" + e.getClass().getSimpleName() + ": " + e.getMessage() + ")");
          } else {
            Log.i(TAG, "[sendGroupMessage][" + timestamp + "] Pipe unavailable (" + e.getClass().getSimpleName() + ": " + e.getMessage() + ")");
            throw e;
          }
        } catch (IOException e) {
          if (useRestFallback.getAsBoolean()) {
            Log.w(TAG, "[sendGroupMessage][" + timestamp + "] Pipe failed, falling back... (" + e.getClass().getSimpleName() + ": " + e.getMessage() + ")");
          } else {
            Log.w(TAG, "[sendGroupMessage][" + timestamp + "] Pipe failed (" + e.getClass().getSimpleName() + ": " + e.getMessage() + ")");
            throw e;
          }
        }

        SendGroupMessageResponse response = socket.sendGroupMessage(ciphertext, sealedSenderAccess, timestamp, online, urgent, story);
        return transformGroupResponseToMessageResults(targetInfo.devices, response, content);
      } catch (GroupMismatchedDevicesException e) {
        Log.w(TAG, "[sendGroupMessage][" + timestamp + "] Handling mismatched devices. (" + e.getMessage() + ")");
        for (GroupMismatchedDevices mismatched : e.getMismatchedDevices()) {
          ZonaRosaServiceAddress address = new ZonaRosaServiceAddress(ServiceId.parseOrThrow(mismatched.getUuid()), Optional.empty());
          handleMismatchedDevices(address, mismatched.getDevices());
        }
      } catch (GroupStaleDevicesException e) {
        Log.w(TAG, "[sendGroupMessage][" + timestamp + "] Handling stale devices. (" + e.getMessage() + ")");
        for (GroupStaleDevices stale : e.getStaleDevices()) {
          ZonaRosaServiceAddress address = new ZonaRosaServiceAddress(ServiceId.parseOrThrow(stale.getUuid()), Optional.empty());
          handleStaleDevices(address, stale.getDevices());
        }
      } catch (InvalidUnidentifiedAccessHeaderException e) {
        Log.w(TAG, "[sendGroupMessage][" + timestamp + "] Invalid access header. (" + e.getMessage() + ")");
        throw e;
      }

      Log.w(TAG, "[sendGroupMessage][" + timestamp + "] Attempt failed (i = " + i + ")");
    }

    throw new IOException("Failed to resolve conflicts after " + RETRY_COUNT + " attempts!");
  }

  private GroupTargetInfo buildGroupTargetInfo(List<ZonaRosaServiceAddress> recipients) {
    List<String>                              addressNames         = recipients.stream().map(ZonaRosaServiceAddress::getIdentifier).collect(Collectors.toList());
    Map<ZonaRosaProtocolAddress, SessionRecord> sessionMap           = aciStore.getAllAddressesWithActiveSessions(addressNames);
    Map<String, List<Integer>>                devicesByAddressName = new HashMap<>();

    Set<ZonaRosaProtocolAddress> destinations = new HashSet<>(sessionMap.keySet());

    destinations.addAll(recipients.stream()
                                  .map(a -> new ZonaRosaProtocolAddress(a.getIdentifier(), ZonaRosaServiceAddress.DEFAULT_DEVICE_ID))
                                  .collect(Collectors.toList()));

    for (ZonaRosaProtocolAddress destination : destinations) {
      List<Integer> devices = devicesByAddressName.containsKey(destination.getName()) ? devicesByAddressName.get(destination.getName()) : new LinkedList<>();
      devices.add(destination.getDeviceId());
      devicesByAddressName.put(destination.getName(), devices);
    }

    Map<ZonaRosaServiceAddress, List<Integer>> recipientDevices = new HashMap<>();

    for (ZonaRosaServiceAddress recipient : recipients) {
      if (devicesByAddressName.containsKey(recipient.getIdentifier())) {
        recipientDevices.put(recipient, devicesByAddressName.get(recipient.getIdentifier()));
      }
    }

    return new GroupTargetInfo(new ArrayList<>(destinations), recipientDevices, sessionMap);
  }


  private static final class GroupTargetInfo {
    private final List<ZonaRosaProtocolAddress>               destinations;
    private final Map<ZonaRosaServiceAddress, List<Integer>>  devices;
    private final Map<ZonaRosaProtocolAddress, SessionRecord> sessions;

    private GroupTargetInfo(
        List<ZonaRosaProtocolAddress> destinations,
        Map<ZonaRosaServiceAddress, List<Integer>> devices,
        Map<ZonaRosaProtocolAddress, SessionRecord> sessions) {
      this.destinations = destinations;
      this.devices      = devices;
      this.sessions     = sessions;
    }
  }

  private List<SendMessageResult> transformGroupResponseToMessageResults(Map<ZonaRosaServiceAddress, List<Integer>> recipients, SendGroupMessageResponse response, Content content) {
    Set<ServiceId> unregistered = response.getUnsentTargets();

    List<SendMessageResult> failures = unregistered.stream()
                                                   .map(ZonaRosaServiceAddress::new)
                                                   .map(SendMessageResult::unregisteredFailure)
                                                   .collect(Collectors.toList());

    List<SendMessageResult> success = recipients.keySet()
                                                .stream()
                                                .filter(r -> !unregistered.contains(r.getServiceId()))
                                                .map(a -> SendMessageResult.success(a, recipients.get(a), true, aciStore.isMultiDevice(), -1, Optional.of(content)))
                                                .collect(Collectors.toList());

    List<SendMessageResult> results = new ArrayList<>(success.size() + failures.size());
    results.addAll(success);
    results.addAll(failures);

    return results;
  }

  private List<AttachmentPointer> createAttachmentPointers(Optional<List<ZonaRosaServiceAttachment>> attachments) throws IOException {
    List<AttachmentPointer> pointers = new LinkedList<>();

    if (!attachments.isPresent() || attachments.get().isEmpty()) {
      return pointers;
    }

    for (ZonaRosaServiceAttachment attachment : attachments.get()) {
      if (attachment.isStream()) {
        Log.i(TAG, "Found attachment, creating pointer...");
        pointers.add(createAttachmentPointer(attachment.asStream()));
      } else if (attachment.isPointer()) {
        Log.i(TAG, "Including existing attachment pointer...");
        pointers.add(createAttachmentPointer(attachment.asPointer()));
      }
    }

    return pointers;
  }

  private AttachmentPointer createAttachmentPointer(ZonaRosaServiceAttachmentPointer attachment) {
    return AttachmentPointerUtil.createAttachmentPointer(attachment);
  }

  private AttachmentPointer createAttachmentPointer(ZonaRosaServiceAttachmentStream attachment)
      throws IOException
  {
    return createAttachmentPointer(uploadAttachment(attachment));
  }

  private TextAttachment createTextAttachment(ZonaRosaServiceTextAttachment attachment) throws IOException {
    TextAttachment.Builder builder = new TextAttachment.Builder();

    if (attachment.getStyle().isPresent()) {
      switch (attachment.getStyle().get()) {
        case DEFAULT:
          builder.textStyle(TextAttachment.Style.DEFAULT);
          break;
        case REGULAR:
          builder.textStyle(TextAttachment.Style.REGULAR);
          break;
        case BOLD:
          builder.textStyle(TextAttachment.Style.BOLD);
          break;
        case SERIF:
          builder.textStyle(TextAttachment.Style.SERIF);
          break;
        case SCRIPT:
          builder.textStyle(TextAttachment.Style.SCRIPT);
          break;
        case CONDENSED:
          builder.textStyle(TextAttachment.Style.CONDENSED);
          break;
        default:
          throw new AssertionError("Unknown type: " + attachment.getStyle().get());
      }
    }

    TextAttachment.Gradient.Builder gradientBuilder = new TextAttachment.Gradient.Builder();

    if (attachment.getBackgroundGradient().isPresent()) {
      ZonaRosaServiceTextAttachment.Gradient gradient = attachment.getBackgroundGradient().get();

      if (gradient.getAngle().isPresent()) gradientBuilder.angle(gradient.getAngle().get());

      if (!gradient.getColors().isEmpty()) {
        gradientBuilder.startColor(gradient.getColors().get(0));
        gradientBuilder.endColor(gradient.getColors().get(gradient.getColors().size() - 1));
      }

      gradientBuilder.colors = gradient.getColors();
      gradientBuilder.positions = gradient.getPositions();

      builder.gradient(gradientBuilder.build());
    }

    if (attachment.getText().isPresent())                builder.text(attachment.getText().get());
    if (attachment.getTextForegroundColor().isPresent()) builder.textForegroundColor(attachment.getTextForegroundColor().get());
    if (attachment.getTextBackgroundColor().isPresent()) builder.textBackgroundColor(attachment.getTextBackgroundColor().get());
    if (attachment.getPreview().isPresent())             builder.preview(createPreview(attachment.getPreview().get()));
    if (attachment.getBackgroundColor().isPresent())     builder.color(attachment.getBackgroundColor().get());

    return builder.build();
  }

  private OutgoingPushMessageList getEncryptedMessages(ZonaRosaServiceAddress         recipient,
                                                       @Nullable SealedSenderAccess sealedSenderAccess,
                                                       long                         timestamp,
                                                       EnvelopeContent              plaintext,
                                                       boolean                      online,
                                                       boolean                      urgent,
                                                       boolean                      story)
      throws IOException, InvalidKeyException, UntrustedIdentityException
  {
    List<OutgoingPushMessage> messages = new LinkedList<>();

    List<Integer> subDevices = aciStore.getSubDeviceSessions(recipient.getIdentifier());

    List<Integer> deviceIds = new ArrayList<>(subDevices.size() + 1);
    deviceIds.add(ZonaRosaServiceAddress.DEFAULT_DEVICE_ID);
    deviceIds.addAll(subDevices);

    if (recipient.matches(localAddress)) {
      deviceIds.remove(Integer.valueOf(localDeviceId));
    }

    for (int deviceId : deviceIds) {
      if (deviceId == ZonaRosaServiceAddress.DEFAULT_DEVICE_ID || aciStore.containsSession(new ZonaRosaProtocolAddress(recipient.getIdentifier(), deviceId))) {
        messages.add(getEncryptedMessage(recipient, sealedSenderAccess, deviceId, plaintext, story));
      }
    }

    return new OutgoingPushMessageList(recipient.getIdentifier(), timestamp, messages, online, urgent);
  }

  // Visible for testing only
  public OutgoingPushMessage getEncryptedMessage(ZonaRosaServiceAddress         recipient,
                                                 @Nullable SealedSenderAccess sealedSenderAccess,
                                                 int                          deviceId,
                                                 EnvelopeContent              plaintext,
                                                 boolean                      story)
      throws IOException, InvalidKeyException, UntrustedIdentityException
  {
    ZonaRosaProtocolAddress zonarosaProtocolAddress = new ZonaRosaProtocolAddress(recipient.getIdentifier(), deviceId);
    ZonaRosaServiceCipher   cipher                = new ZonaRosaServiceCipher(localAddress, localDeviceId, aciStore, sessionLock, null);

    if (!aciStore.containsSession(zonarosaProtocolAddress)) {
      try {
        List<PreKeyBundle> preKeys = getPreKeys(recipient, sealedSenderAccess, deviceId, story);

        for (PreKeyBundle preKey : preKeys) {
          Log.d(TAG, "Initializing prekey session for " + zonarosaProtocolAddress);

          try {
            ZonaRosaProtocolAddress preKeyAddress  = new ZonaRosaProtocolAddress(recipient.getIdentifier(), preKey.getDeviceId());
            ZonaRosaSessionBuilder  sessionBuilder = new ZonaRosaSessionBuilder(sessionLock, new SessionBuilder(aciStore, preKeyAddress));
            sessionBuilder.process(preKey);
          } catch (io.zonarosa.libzonarosa.protocol.UntrustedIdentityException e) {
            throw new UntrustedIdentityException("Untrusted identity key!", recipient.getIdentifier(), preKey.getIdentityKey());
          }
        }

        if (eventListener.isPresent()) {
          eventListener.get().onSecurityEvent(recipient);
        }
      } catch (InvalidKeyException e) {
        throw new InvalidPreKeyException(zonarosaProtocolAddress, e);
      }
    }

    try {
      return cipher.encrypt(zonarosaProtocolAddress, sealedSenderAccess, plaintext);
    } catch (io.zonarosa.libzonarosa.protocol.UntrustedIdentityException e) {
      throw new UntrustedIdentityException("Untrusted on send", recipient.getIdentifier(), e.getUntrustedIdentity());
    }
  }

  private void eagerlyFetchMissingPreKeys(List<ZonaRosaServiceAddress> recipients, List<SealedSenderAccess> sealedSenderAccesses, boolean story) {
    long start = System.currentTimeMillis();

    Iterator<ZonaRosaServiceAddress> recipientIterator          = recipients.iterator();
    Iterator<SealedSenderAccess>   sealedSenderAccessIterator = sealedSenderAccesses.iterator();
    List<Observable<Boolean>>      eagerFetches               = new LinkedList<>();

    while (recipientIterator.hasNext()) {
      ZonaRosaServiceAddress  recipient             = recipientIterator.next();
      SealedSenderAccess    sealedSenderAccess    = sealedSenderAccessIterator.next();
      ZonaRosaProtocolAddress zonarosaProtocolAddress = new ZonaRosaProtocolAddress(recipient.getIdentifier(), ZonaRosaServiceAddress.DEFAULT_DEVICE_ID);

      if (!aciStore.containsSession(zonarosaProtocolAddress)) {
        Observable<Boolean> thing = Single.fromCallable(() -> {
                                            eagerlyFetchMissingPreKeys(recipient, sealedSenderAccess, story);
                                            return true;
                                          })
                                          .subscribeOn(scheduler)
                                          .toObservable();

        eagerFetches.add(thing);
      }
    }

    if (eagerFetches.isEmpty()) {
      return;
    }

    Log.i(TAG, "[eagerPrefetch] Attempting to fetch prekeys for " + eagerFetches.size() + " recipients");

    try {
      //noinspection ResultOfMethodCallIgnored
      Observable.mergeDelayError(eagerFetches, Integer.MAX_VALUE, 1)
                .observeOn(scheduler)
                .lastOrError()
                .blockingGet();
    } catch (RuntimeException e) {
      Log.w(TAG, "[eagerPrefetch] Unexpectedly failed eager fetching prekeys", e);
      return;
    }

    Log.i(TAG, "[eagerPrefetch] Completed in " + (System.currentTimeMillis() - start) + "ms");
  }

  private void eagerlyFetchMissingPreKeys(ZonaRosaServiceAddress recipient, SealedSenderAccess sealedSenderAccess, boolean story) {
    ZonaRosaProtocolAddress zonarosaProtocolAddress = new ZonaRosaProtocolAddress(recipient.getIdentifier(), ZonaRosaServiceAddress.DEFAULT_DEVICE_ID);

    try {
      List<PreKeyBundle> preKeys = getPreKeys(recipient, sealedSenderAccess, ZonaRosaServiceAddress.DEFAULT_DEVICE_ID, story);

      for (PreKeyBundle preKey : preKeys) {
        Log.d(TAG, "[eagerFetch] Initializing prekey session for " + zonarosaProtocolAddress);

        try {
          ZonaRosaProtocolAddress preKeyAddress  = new ZonaRosaProtocolAddress(recipient.getIdentifier(), preKey.getDeviceId());
          ZonaRosaSessionBuilder  sessionBuilder = new ZonaRosaSessionBuilder(sessionLock, new SessionBuilder(aciStore, preKeyAddress));
          sessionBuilder.process(preKey);
        } catch (io.zonarosa.libzonarosa.protocol.UntrustedIdentityException e) {
          Log.i(TAG, "[eagerPrefetch] Untrusted identity for recipient");
          return;

        }
      }

      if (eventListener.isPresent()) {
        eventListener.get().onSecurityEvent(recipient);
      }
    } catch (IOException e) {
      Log.i(TAG, "[eagerPrefetch] Network issue encountered");
    } catch (InvalidKeyException e) {
      Log.i(TAG, "[eagerPrefetch] Invalid pre-key");
      return;
    }
  }

  private List<PreKeyBundle> getPreKeys(ZonaRosaServiceAddress recipient, @Nullable SealedSenderAccess sealedSenderAccess, int deviceId, boolean story) throws IOException {
    try {
      // If it's only unrestricted because it's a story send, then we know it'll fail
      if (story && SealedSenderAccess.isUnrestrictedForStory(sealedSenderAccess)) {
        sealedSenderAccess = null;
      }

      return NetworkResultUtil.toPreKeysLegacy(keysApi.getPreKeys(recipient, sealedSenderAccess, deviceId));
    } catch (NonSuccessfulResponseCodeException e) {
      if (e.code == 401 && story) {
        Log.d(TAG, "Got 401 when fetching prekey for story. Trying without UD.");
        return NetworkResultUtil.toPreKeysLegacy(keysApi.getPreKeys(recipient, null, deviceId));
      } else {
        throw e;
      }
    }
  }

  private void handleMismatchedDevices(ZonaRosaServiceAddress recipient,
                                       MismatchedDevices mismatchedDevices)
      throws IOException, UntrustedIdentityException
  {
    try {
      Log.w(TAG, "[handleMismatchedDevices] Address: " + recipient.getIdentifier() + ", ExtraDevices: " + mismatchedDevices.getExtraDevices() + ", MissingDevices: " + mismatchedDevices.getMissingDevices());
      archiveSessions(recipient, mismatchedDevices.getExtraDevices());

      ArrayList<Integer> mismatchedDeviceIds = new ArrayList<>();
      mismatchedDeviceIds.addAll(mismatchedDevices.getExtraDevices());
      mismatchedDeviceIds.addAll(mismatchedDevices.getMissingDevices());
      clearSenderKeySharedWith(recipient, mismatchedDeviceIds);

      for (int missingDeviceId : mismatchedDevices.getMissingDevices()) {
        PreKeyBundle preKey = NetworkResultUtil.toPreKeysLegacy(keysApi.getPreKey(recipient, missingDeviceId));

        try {
          ZonaRosaSessionBuilder sessionBuilder = new ZonaRosaSessionBuilder(sessionLock, new SessionBuilder(aciStore, new ZonaRosaProtocolAddress(recipient.getIdentifier(), missingDeviceId)));
          sessionBuilder.process(preKey);
        } catch (io.zonarosa.libzonarosa.protocol.UntrustedIdentityException e) {
          throw new UntrustedIdentityException("Untrusted identity key!", recipient.getIdentifier(), preKey.getIdentityKey());
        }
      }
    } catch (InvalidKeyException e) {
      throw new IOException(e);
    }
  }

  private void handleStaleDevices(ZonaRosaServiceAddress recipient, StaleDevices staleDevices) {
    Log.w(TAG, "[handleStaleDevices] Address: " + recipient.getIdentifier() + ", StaleDevices: " + staleDevices.getStaleDevices());
    archiveSessions(recipient, staleDevices.getStaleDevices());
    clearSenderKeySharedWith(recipient, staleDevices.getStaleDevices());
  }

  public void handleChangeNumberMismatchDevices(@Nonnull MismatchedDevices mismatchedDevices)
      throws IOException, UntrustedIdentityException
  {
    handleMismatchedDevices(localAddress, mismatchedDevices);
  }

  private void archiveSessions(ZonaRosaServiceAddress recipient, List<Integer> devices) {
    List<ZonaRosaProtocolAddress> addressesToClear = convertToProtocolAddresses(recipient, devices);

    for (ZonaRosaProtocolAddress address : addressesToClear) {
      aciStore.archiveSession(address);
    }
  }

  private void clearSenderKeySharedWith(ZonaRosaServiceAddress recipient, List<Integer> deviceIds) {
    aciStore.clearSenderKeySharedWith(convertToProtocolAddresses(recipient, deviceIds));
  }

  private List<ZonaRosaProtocolAddress> convertToProtocolAddresses(ZonaRosaServiceAddress recipient, List<Integer> devices) {
    List<ZonaRosaProtocolAddress> addresses = new ArrayList<>(devices.size());

    for (int staleDeviceId : devices) {
      addresses.add(new ZonaRosaProtocolAddress(recipient.getServiceId().toString(), staleDeviceId));

      if (recipient.getNumber().isPresent()) {
        addresses.add(new ZonaRosaProtocolAddress(recipient.getNumber().get(), staleDeviceId));
      }
    }

    return addresses;
  }

  private void enforceMaxEnvelopeContentSize(EnvelopeContent content) {
    int size = content.size();

    if (maxEnvelopeSize > 0 && size > maxEnvelopeSize) {
      String message;
      if (content.getContent().isEmpty()) {
        message = "Empty content";
      } else {
        message = buildContentTooLargeBreadcrumbs(content.getContent().get());
      }
      Log.w(TAG, "About to crash for exceeding max envelope size (" + size + " > " + maxEnvelopeSize + ")\n" + message);
      throw new ContentTooLargeException(size, message);
    }
  }

  private Content enforceMaxContentSize(Content content) {
    int size = content.encode().length;

    if (maxEnvelopeSize > 0 && size > maxEnvelopeSize) {
      throw new ContentTooLargeException(size, buildContentTooLargeBreadcrumbs(content));
    }

    return content;
  }

  private String buildContentTooLargeBreadcrumbs(Content content) {
    return ProtoUtil.buildSizeTree(content, "Content");
  }

  public interface EventListener {
    void onSecurityEvent(ZonaRosaServiceAddress address);
  }

  public interface SendEvents {
    void onMessageEncrypted();
    void onMessageSent();
    void onSyncMessageSent();
  }

  public interface IndividualSendEvents extends SendEvents {
    IndividualSendEvents EMPTY = new IndividualSendEvents() {
      @Override
      public void onMessageEncrypted() { }

      @Override
      public void onMessageSent() { }

      @Override
      public void onSyncMessageSent() { }
    };
  }

  public interface SenderKeyGroupEvents extends SendEvents {
    SenderKeyGroupEvents EMPTY = new SenderKeyGroupEvents() {
      @Override
      public void onSenderKeyShared() { }

      @Override
      public void onMessageEncrypted() { }

      @Override
      public void onMessageSent() { }

      @Override
      public void onSyncMessageSent() { }
    };

    void onSenderKeyShared();
  }

  public interface LegacyGroupEvents extends SendEvents {
    LegacyGroupEvents EMPTY = new LegacyGroupEvents() {
      @Override
      public void onMessageEncrypted() {}

      @Override
      public void onMessageSent() { }

      @Override
      public void onSyncMessageSent() { }
    };

  }
}
