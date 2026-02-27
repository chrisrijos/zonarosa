package io.zonarosa.messenger.jobs;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.zonarosa.core.util.logging.Log;
import io.zonarosa.libzonarosa.zkgroup.groupsend.GroupSendFullToken;
import io.zonarosa.messenger.crypto.SealedSenderAccessUtil;
import io.zonarosa.messenger.database.ZonaRosaDatabase;
import io.zonarosa.messenger.database.model.MessageId;
import io.zonarosa.messenger.dependencies.AppDependencies;
import io.zonarosa.messenger.jobmanager.Job;
import io.zonarosa.messenger.jobmanager.JsonJobData;
import io.zonarosa.messenger.jobmanager.impl.NetworkConstraint;
import io.zonarosa.messenger.net.NotPushRegisteredException;
import io.zonarosa.messenger.recipients.Recipient;
import io.zonarosa.messenger.recipients.RecipientId;
import io.zonarosa.messenger.recipients.RecipientUtil;
import io.zonarosa.messenger.transport.UndeliverableMessageException;
import io.zonarosa.service.api.ZonaRosaServiceMessageSender;
import io.zonarosa.service.api.crypto.ContentHint;
import io.zonarosa.service.api.crypto.UntrustedIdentityException;
import io.zonarosa.service.api.messages.SendMessageResult;
import io.zonarosa.service.api.messages.ZonaRosaServiceReceiptMessage;
import io.zonarosa.service.api.push.ZonaRosaServiceAddress;
import io.zonarosa.service.api.push.exceptions.PushNetworkException;
import io.zonarosa.service.api.push.exceptions.ServerRejectedException;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class SendDeliveryReceiptJob extends BaseJob {

  public static final String KEY = "SendDeliveryReceiptJob";

  private static final String KEY_RECIPIENT              = "recipient";
  private static final String KEY_MESSAGE_SENT_TIMESTAMP = "message_id";
  private static final String KEY_TIMESTAMP              = "timestamp";
  private static final String KEY_MESSAGE_ID             = "message_db_id";

  private static final String TAG = Log.tag(SendReadReceiptJob.class);

  private final RecipientId recipientId;
  private final long        messageSentTimestamp;
  private final long        timestamp;

  @Nullable
  private final MessageId messageId;

  public SendDeliveryReceiptJob(@NonNull RecipientId recipientId, long messageSentTimestamp, @NonNull MessageId messageId) {
    this(new Job.Parameters.Builder()
                           .addConstraint(NetworkConstraint.KEY)
                           .setLifespan(TimeUnit.DAYS.toMillis(1))
                           .setMaxAttempts(Parameters.UNLIMITED)
                           .setQueue(recipientId.toQueueKey())
                           .build(),
         recipientId,
         messageSentTimestamp,
         messageId,
         System.currentTimeMillis());
  }

  private SendDeliveryReceiptJob(@NonNull Job.Parameters parameters,
                                 @NonNull RecipientId recipientId,
                                 long messageSentTimestamp,
                                 @Nullable MessageId messageId,
                                 long timestamp)
  {
    super(parameters);

    this.recipientId          = recipientId;
    this.messageSentTimestamp = messageSentTimestamp;
    this.messageId            = messageId;
    this.timestamp            = timestamp;
  }

  @Override
  public @Nullable byte[] serialize() {
    JsonJobData.Builder builder = new JsonJobData.Builder().putString(KEY_RECIPIENT, recipientId.serialize())
                                                           .putLong(KEY_MESSAGE_SENT_TIMESTAMP, messageSentTimestamp)
                                                           .putLong(KEY_TIMESTAMP, timestamp);

    if (messageId != null) {
      builder.putString(KEY_MESSAGE_ID, messageId.serialize());
    }

    return builder.serialize();
  }

  @Override
  public @NonNull String getFactoryKey() {
    return KEY;
  }

  @Override
  public void onRun() throws IOException, UntrustedIdentityException, UndeliverableMessageException {
    if (!Recipient.self().isRegistered()) {
      throw new NotPushRegisteredException();
    }

    ZonaRosaServiceMessageSender  messageSender  = AppDependencies.getZonaRosaServiceMessageSender();
    Recipient                   recipient      = Recipient.resolved(recipientId);

    if (recipient.isSelf()) {
      Log.i(TAG, "Not sending to self, abort");
      return;
    }

    if (recipient.isUnregistered()) {
      Log.w(TAG, recipient.getId() + " is unregistered!");
      return;
    }

    if (!recipient.getHasServiceId() && !recipient.getHasE164()) {
      Log.w(TAG, "No serviceId or e164!");
      return;
    }

    ZonaRosaServiceAddress        remoteAddress  = RecipientUtil.toZonaRosaServiceAddress(context, recipient);
    ZonaRosaServiceReceiptMessage receiptMessage = new ZonaRosaServiceReceiptMessage(ZonaRosaServiceReceiptMessage.Type.DELIVERY,
                                                                                 Collections.singletonList(messageSentTimestamp),
                                                                                 timestamp);

    SendMessageResult result = messageSender.sendReceipt(remoteAddress,
                                                         SealedSenderAccessUtil.getSealedSenderAccessFor(recipient, this::getGroupSendFullToken),
                                                         receiptMessage,
                                                         recipient.getNeedsPniSignature());

    if (messageId != null) {
      ZonaRosaDatabase.messageLog().insertIfPossible(recipientId, timestamp, result, ContentHint.IMPLICIT, messageId, false);
    }
  }

  private @Nullable GroupSendFullToken getGroupSendFullToken() {
    if (messageId == null) {
      return null;
    }

    long threadId = ZonaRosaDatabase.messages().getThreadIdForMessage(messageId.getId());
    if (threadId == -1) {
      return null;
    }

    return ZonaRosaDatabase.groups().getGroupSendFullToken(threadId, recipientId);
  }

  @Override
  public boolean onShouldRetry(@NonNull Exception e) {
    if (e instanceof ServerRejectedException) return false;
    if (e instanceof PushNetworkException) return true;
    return false;
  }

  @Override
  public void onFailure() {
    Log.w(TAG, "Failed to send delivery receipt to: " + recipientId);
  }

  public static final class Factory implements Job.Factory<SendDeliveryReceiptJob> {
    @Override
    public @NonNull SendDeliveryReceiptJob create(@NonNull Parameters parameters, @Nullable byte[] serializedData) {
      JsonJobData data = JsonJobData.deserialize(serializedData);

      MessageId messageId = null;

      if (data.hasString(KEY_MESSAGE_ID)) {
        messageId = MessageId.deserialize(data.getString(KEY_MESSAGE_ID));
      }

      return new SendDeliveryReceiptJob(parameters,
                                        RecipientId.from(data.getString(KEY_RECIPIENT)),
                                        data.getLong(KEY_MESSAGE_SENT_TIMESTAMP),
                                        messageId,
                                        data.getLong(KEY_TIMESTAMP));
    }
  }
}
