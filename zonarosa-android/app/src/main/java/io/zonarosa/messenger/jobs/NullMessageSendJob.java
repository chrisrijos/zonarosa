package io.zonarosa.messenger.jobs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.zonarosa.core.util.logging.Log;
import io.zonarosa.messenger.crypto.SealedSenderAccessUtil;
import io.zonarosa.messenger.dependencies.AppDependencies;
import io.zonarosa.messenger.jobmanager.Job;
import io.zonarosa.messenger.jobmanager.JsonJobData;
import io.zonarosa.messenger.jobmanager.impl.NetworkConstraint;
import io.zonarosa.messenger.recipients.Recipient;
import io.zonarosa.messenger.recipients.RecipientId;
import io.zonarosa.messenger.recipients.RecipientUtil;
import io.zonarosa.service.api.ZonaRosaServiceMessageSender;
import io.zonarosa.service.api.crypto.UntrustedIdentityException;
import io.zonarosa.service.api.push.ZonaRosaServiceAddress;
import io.zonarosa.service.api.push.exceptions.PushNetworkException;

import java.util.concurrent.TimeUnit;

/**
 * Just sends an empty message to a target recipient. Only suitable for individuals, NOT groups.
 */
public class NullMessageSendJob extends BaseJob {

  public static final String KEY = "NullMessageSendJob";

  private static final String TAG = Log.tag(NullMessageSendJob.class);

  private final RecipientId recipientId;

  private static final String KEY_RECIPIENT_ID = "recipient_id";

  public NullMessageSendJob(@NonNull RecipientId recipientId) {
    this(recipientId,
         new Parameters.Builder()
                       .setQueue(recipientId.toQueueKey())
                       .addConstraint(NetworkConstraint.KEY)
                       .setLifespan(TimeUnit.DAYS.toMillis(1))
                       .setMaxAttempts(Parameters.UNLIMITED)
                       .build());
  }

  private NullMessageSendJob(@NonNull RecipientId recipientId, @NonNull Parameters parameters) {
    super(parameters);
    this.recipientId = recipientId;
  }

  @Override
  public @Nullable byte[] serialize() {
    return new JsonJobData.Builder().putString(KEY_RECIPIENT_ID, recipientId.serialize()).serialize();
  }

  @Override
  public @NonNull String getFactoryKey() {
    return KEY;
  }

  @Override
  protected void onRun() throws Exception {
    Recipient recipient = Recipient.resolved(recipientId);

    if (recipient.isGroup()) {
      Log.w(TAG, "Groups are not supported!");
      return;
    }

    if (recipient.isUnregistered()) {
      Log.w(TAG, recipient.getId() + " not registered!");
    }

    ZonaRosaServiceMessageSender messageSender = AppDependencies.getZonaRosaServiceMessageSender();
    ZonaRosaServiceAddress       address       = RecipientUtil.toZonaRosaServiceAddress(context, recipient);

    try {
      messageSender.sendNullMessage(address, SealedSenderAccessUtil.getSealedSenderAccessFor(recipient));
    } catch (UntrustedIdentityException e) {
      Log.w(TAG, "Unable to send null message.");
    }
  }

  @Override
  protected boolean onShouldRetry(@NonNull Exception e) {
    return e instanceof PushNetworkException;
  }

  @Override
  public void onFailure() {
  }

  public static final class Factory implements Job.Factory<NullMessageSendJob> {

    @Override
    public @NonNull NullMessageSendJob create(@NonNull Parameters parameters, @Nullable byte[] serializedData) {
      JsonJobData data = JsonJobData.deserialize(serializedData);

      return new NullMessageSendJob(RecipientId.from(data.getString(KEY_RECIPIENT_ID)),
                                    parameters);
    }
  }
}
