package io.zonarosa.messenger.jobs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.annimon.stream.Stream;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.zonarosa.core.util.ListUtil;
import io.zonarosa.core.util.logging.Log;
import io.zonarosa.messenger.database.MessageTable.SyncMessageId;
import io.zonarosa.messenger.dependencies.AppDependencies;
import io.zonarosa.messenger.jobmanager.Job;
import io.zonarosa.messenger.jobmanager.JobManager;
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
import io.zonarosa.service.api.messages.multidevice.ViewedMessage;
import io.zonarosa.service.api.push.exceptions.PushNetworkException;
import io.zonarosa.service.api.push.exceptions.ServerRejectedException;

import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MultiDeviceViewedUpdateJob extends BaseJob {

  public static final String KEY = "MultiDeviceViewedUpdateJob";

  private static final String TAG = Log.tag(MultiDeviceViewedUpdateJob.class);

  private static final String KEY_MESSAGE_IDS = "message_ids";

  private List<SerializableSyncMessageId> messageIds;

  private MultiDeviceViewedUpdateJob(List<SyncMessageId> messageIds) {
    this(new Parameters.Builder()
                       .addConstraint(NetworkConstraint.KEY)
                       .setLifespan(TimeUnit.DAYS.toMillis(1))
                       .setMaxAttempts(Parameters.UNLIMITED)
                       .build(),
         SendReadReceiptJob.ensureSize(messageIds, SendReadReceiptJob.MAX_TIMESTAMPS));
  }

  private MultiDeviceViewedUpdateJob(@NonNull Parameters parameters, @NonNull List<SyncMessageId> messageIds) {
    super(parameters);

    this.messageIds = new LinkedList<>();

    for (SyncMessageId messageId : messageIds) {
      this.messageIds.add(new SerializableSyncMessageId(messageId.getRecipientId().serialize(), messageId.getTimetamp()));
    }
  }

  /**
   * Enqueues all the necessary jobs for read receipts, ensuring that they're all within the
   * maximum size.
   */
  public static void enqueue(@NonNull List<SyncMessageId> messageIds) {
    JobManager                jobManager      = AppDependencies.getJobManager();
    List<List<SyncMessageId>> messageIdChunks = ListUtil.chunk(messageIds, SendReadReceiptJob.MAX_TIMESTAMPS);

    if (messageIdChunks.size() > 1) {
      Log.w(TAG, "Large receipt count! Had to break into multiple chunks. Total count: " + messageIds.size());
    }

    for (List<SyncMessageId> chunk : messageIdChunks) {
      jobManager.add(new MultiDeviceViewedUpdateJob(chunk));
    }
  }

  @Override
  public @Nullable byte[] serialize() {
    String[] ids = new String[messageIds.size()];

    for (int i = 0; i < ids.length; i++) {
      try {
        ids[i] = JsonUtils.toJson(messageIds.get(i));
      } catch (IOException e) {
        throw new AssertionError(e);
      }
    }

    return new JsonJobData.Builder().putStringArray(KEY_MESSAGE_IDS, ids).serialize();
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

    List<ViewedMessage> viewedMessages = new LinkedList<>();

    for (SerializableSyncMessageId messageId : messageIds) {
      Recipient recipient = Recipient.resolved(RecipientId.from(messageId.recipientId));
      if (!recipient.isGroup() && recipient.isMaybeRegistered()) {
        viewedMessages.add(new ViewedMessage(RecipientUtil.getOrFetchServiceId(context, recipient), messageId.timestamp));
      }
    }

    ZonaRosaServiceMessageSender messageSender = AppDependencies.getZonaRosaServiceMessageSender();
    messageSender.sendSyncMessage(ZonaRosaServiceSyncMessage.forViewed(viewedMessages));
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

  public static final class Factory implements Job.Factory<MultiDeviceViewedUpdateJob> {
    @Override
    public @NonNull MultiDeviceViewedUpdateJob create(@NonNull Parameters parameters, @Nullable byte[] serializedData) {
      JsonJobData data = JsonJobData.deserialize(serializedData);

      List<SyncMessageId> ids = Stream.of(data.getStringArray(KEY_MESSAGE_IDS))
                                      .map(id -> {
                                        try {
                                          return JsonUtils.fromJson(id, SerializableSyncMessageId.class);
                                        } catch (IOException e) {
                                          throw new AssertionError(e);
                                        }
                                      })
                                      .map(id -> new SyncMessageId(RecipientId.from(id.recipientId), id.timestamp))
                                      .toList();

      return new MultiDeviceViewedUpdateJob(parameters, ids);
    }
  }
}
