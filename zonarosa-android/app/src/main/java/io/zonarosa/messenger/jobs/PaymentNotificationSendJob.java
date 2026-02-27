package io.zonarosa.messenger.jobs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.zonarosa.core.util.logging.Log;
import io.zonarosa.messenger.crypto.SealedSenderAccessUtil;
import io.zonarosa.messenger.database.PaymentTable;
import io.zonarosa.messenger.database.ZonaRosaDatabase;
import io.zonarosa.messenger.dependencies.AppDependencies;
import io.zonarosa.messenger.jobmanager.Job;
import io.zonarosa.messenger.jobmanager.JsonJobData;
import io.zonarosa.messenger.net.NotPushRegisteredException;
import io.zonarosa.messenger.recipients.Recipient;
import io.zonarosa.messenger.recipients.RecipientId;
import io.zonarosa.messenger.recipients.RecipientUtil;
import io.zonarosa.messenger.transport.RetryLaterException;
import io.zonarosa.service.api.ZonaRosaServiceMessageSender;
import io.zonarosa.service.api.ZonaRosaServiceMessageSender.IndividualSendEvents;
import io.zonarosa.service.api.crypto.ContentHint;
import io.zonarosa.service.api.messages.SendMessageResult;
import io.zonarosa.service.api.messages.ZonaRosaServiceDataMessage;
import io.zonarosa.service.api.push.ZonaRosaServiceAddress;
import io.zonarosa.service.api.push.exceptions.ServerRejectedException;

import java.io.IOException;
import java.util.UUID;

public final class PaymentNotificationSendJob extends BaseJob {

  public static final String KEY = "PaymentNotificationSendJob";

  private static final String TAG = Log.tag(PaymentNotificationSendJob.class);

  private static final String KEY_UUID      = "uuid";
  private static final String KEY_RECIPIENT = "recipient";

  private final RecipientId recipientId;
  private final UUID        uuid;

  public static Job create(@NonNull RecipientId recipientId, @NonNull UUID uuid, @NonNull String queue) {
    return new PaymentNotificationSendJobV2(recipientId, uuid);
  }

  private PaymentNotificationSendJob(@NonNull Parameters parameters,
                                     @NonNull RecipientId recipientId,
                                     @NonNull UUID uuid)
  {
    super(parameters);

    this.recipientId = recipientId;
    this.uuid        = uuid;
  }

  @Override
  public @Nullable byte[] serialize() {
    return new JsonJobData.Builder()
                   .putString(KEY_RECIPIENT, recipientId.serialize())
                   .putString(KEY_UUID, uuid.toString())
                   .serialize();
  }

  @Override
  public @NonNull String getFactoryKey() {
    return KEY;
  }

  @Override
  protected void onRun() throws Exception {
    if (!Recipient.self().isRegistered()) {
      throw new NotPushRegisteredException();
    }

    PaymentTable paymentDatabase = ZonaRosaDatabase.payments();
    Recipient    recipient       = Recipient.resolved(recipientId);

    if (recipient.isUnregistered()) {
      Log.w(TAG, recipientId + " not registered!");
      return;
    }

    ZonaRosaServiceMessageSender messageSender = AppDependencies.getZonaRosaServiceMessageSender();
    ZonaRosaServiceAddress       address       = RecipientUtil.toZonaRosaServiceAddress(context, recipient);

    PaymentTable.PaymentTransaction payment = paymentDatabase.getPayment(uuid);

    if (payment == null) {
      Log.w(TAG, "Could not find payment, cannot send notification " + uuid);
      return;
    }

    if (payment.getReceipt() == null) {
      Log.w(TAG, "Could not find payment receipt, cannot send notification " + uuid);
      return;
    }

    ZonaRosaServiceDataMessage dataMessage = ZonaRosaServiceDataMessage.newBuilder()
                                                                   .withPayment(new ZonaRosaServiceDataMessage.Payment(new ZonaRosaServiceDataMessage.PaymentNotification(payment.getReceipt(), payment.getNote()), null))
                                                                   .build();

    SendMessageResult sendMessageResult = messageSender.sendDataMessage(address,
                                                                        SealedSenderAccessUtil.getSealedSenderAccessFor(recipient),
                                                                        ContentHint.DEFAULT,
                                                                        dataMessage,
                                                                        IndividualSendEvents.EMPTY,
                                                                        false,
                                                                        recipient.getNeedsPniSignature());

    if (recipient.getNeedsPniSignature()) {
      ZonaRosaDatabase.pendingPniSignatureMessages().insertIfNecessary(recipientId, dataMessage.getTimestamp(), sendMessageResult);
    }

    if (sendMessageResult.getIdentityFailure() != null) {
      Log.w(TAG, "Identity failure for " + recipient.getId());
    } else if (sendMessageResult.isUnregisteredFailure()) {
      Log.w(TAG, "Unregistered failure for " + recipient.getId());
    } else if (sendMessageResult.getSuccess() == null) {
      throw new RetryLaterException();
    } else {
      Log.i(TAG, String.format("Payment notification sent to %s for %s", recipientId, uuid));
    }
  }

  @Override
  protected boolean onShouldRetry(@NonNull Exception e) {
    if (e instanceof ServerRejectedException) return false;
    if (e instanceof NotPushRegisteredException) return false;
    return e instanceof IOException ||
           e instanceof RetryLaterException;
  }

  @Override
  public void onFailure() {
    Log.w(TAG, String.format("Failed to send payment notification to recipient %s for %s", recipientId, uuid));
  }

  public static class Factory implements Job.Factory<PaymentNotificationSendJob> {
    @Override
    public @NonNull PaymentNotificationSendJob create(@NonNull Parameters parameters, @Nullable byte[] serializedData) {
      JsonJobData data = JsonJobData.deserialize(serializedData);

      return new PaymentNotificationSendJob(parameters,
                                            RecipientId.from(data.getString(KEY_RECIPIENT)),
                                            UUID.fromString(data.getString(KEY_UUID)));
    }
  }
}
