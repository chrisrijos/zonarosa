package io.zonarosa.messenger.jobs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.zonarosa.core.util.logging.Log;
import io.zonarosa.messenger.database.MessageTable.SyncMessageId;
import io.zonarosa.messenger.dependencies.AppDependencies;
import io.zonarosa.messenger.jobmanager.Job;
import io.zonarosa.messenger.jobmanager.JsonJobData;
import io.zonarosa.messenger.jobmanager.impl.NetworkConstraint;
import io.zonarosa.messenger.keyvalue.ZonaRosaStore;
import io.zonarosa.messenger.net.NotPushRegisteredException;
import io.zonarosa.messenger.recipients.Recipient;
import io.zonarosa.messenger.recipients.RecipientId;
import io.zonarosa.messenger.recipients.RecipientUtil;
import io.zonarosa.messenger.util.JsonUtils;
import io.zonarosa.messenger.util.ZonaRosaPreferences;
import io.zonarosa.service.api.ZonaRosaServiceMessageSender;
import io.zonarosa.service.api.crypto.UntrustedIdentityException;
import io.zonarosa.service.api.messages.multidevice.ZonaRosaServiceSyncMessage;
import io.zonarosa.service.api.messages.multidevice.ViewOnceOpenMessage;
import io.zonarosa.service.api.push.exceptions.PushNetworkException;
import io.zonarosa.service.api.push.exceptions.ServerRejectedException;

import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;

public class MultiDeviceViewOnceOpenJob extends BaseJob {

  public static final String KEY = "MultiDeviceRevealUpdateJob";

  private static final String TAG = Log.tag(MultiDeviceViewOnceOpenJob.class);

  private static final String KEY_MESSAGE_ID = "message_id";

  private SerializableSyncMessageId messageId;

  public MultiDeviceViewOnceOpenJob(SyncMessageId messageId) {
    this(new Parameters.Builder()
                       .addConstraint(NetworkConstraint.KEY)
                       .setLifespan(TimeUnit.DAYS.toMillis(1))
                       .setMaxAttempts(Parameters.UNLIMITED)
                       .build(),
         messageId);
  }

  private MultiDeviceViewOnceOpenJob(@NonNull Parameters parameters, @NonNull SyncMessageId syncMessageId) {
    super(parameters);
    this.messageId = new SerializableSyncMessageId(syncMessageId.getRecipientId().serialize(), syncMessageId.getTimetamp());
  }

  @Override
  public @Nullable byte[] serialize() {
    String serialized;

    try {
      serialized = JsonUtils.toJson(messageId);
    } catch (IOException e) {
      throw new AssertionError(e);
    }

    return new JsonJobData.Builder().putString(KEY_MESSAGE_ID, serialized).serialize();
  }

  @Override
  public @NonNull String getFactoryKey() {
    return KEY;
  }

  @Override
  public void onRun() throws IOException, UntrustedIdentityException {
    if (!Recipient.self().isRegistered()) {
      throw new NotPushRegisteredException();
    }

    if (!ZonaRosaStore.account().isMultiDevice()) {
      Log.i(TAG, "Not multi device...");
      return;
    }

    ZonaRosaServiceMessageSender messageSender = AppDependencies.getZonaRosaServiceMessageSender();
    Recipient                  recipient     = Recipient.resolved(RecipientId.from(messageId.recipientId));

    if (recipient.isUnregistered()) {
      Log.w(TAG, recipient.getId() + " not registered!");
      return;
    }

    ViewOnceOpenMessage openMessage = new ViewOnceOpenMessage(RecipientUtil.getOrFetchServiceId(context, recipient), messageId.timestamp);

    messageSender.sendSyncMessage(ZonaRosaServiceSyncMessage.forViewOnceOpen(openMessage));
  }

  @Override
  public boolean onShouldRetry(@NonNull Exception exception) {
    if (exception instanceof ServerRejectedException) return false;
    return exception instanceof PushNetworkException;
  }

  @Override
  public void onFailure() {

  }

  private static class SerializableSyncMessageId implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty
    private final String recipientId;

    @JsonProperty
    private final long   timestamp;

    private SerializableSyncMessageId(@JsonProperty("recipientId") String recipientId, @JsonProperty("timestamp") long timestamp) {
      this.recipientId = recipientId;
      this.timestamp   = timestamp;
    }
  }

  public static final class Factory implements Job.Factory<MultiDeviceViewOnceOpenJob> {
    @Override
    public @NonNull MultiDeviceViewOnceOpenJob create(@NonNull Parameters parameters, @Nullable byte[] serializedData) {
      JsonJobData data = JsonJobData.deserialize(serializedData);

      SerializableSyncMessageId messageId;
      try {
        messageId = JsonUtils.fromJson(data.getString(KEY_MESSAGE_ID), SerializableSyncMessageId.class);
      } catch (IOException e) {
        throw new AssertionError(e);
      }

      SyncMessageId syncMessageId = new SyncMessageId(RecipientId.from(messageId.recipientId), messageId.timestamp);

      return new MultiDeviceViewOnceOpenJob(parameters, syncMessageId);
    }
  }
}
