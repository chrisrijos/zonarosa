package io.zonarosa.messenger.jobs;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.zonarosa.core.util.Base64;
import io.zonarosa.core.util.logging.Log;
import io.zonarosa.libzonarosa.protocol.IdentityKey;
import io.zonarosa.libzonarosa.protocol.InvalidKeyException;
import io.zonarosa.messenger.database.IdentityTable.VerifiedStatus;
import io.zonarosa.messenger.dependencies.AppDependencies;
import io.zonarosa.messenger.jobmanager.Job;
import io.zonarosa.messenger.jobmanager.JsonJobData;
import io.zonarosa.messenger.jobmanager.impl.NetworkConstraint;
import io.zonarosa.messenger.keyvalue.ZonaRosaStore;
import io.zonarosa.messenger.net.NotPushRegisteredException;
import io.zonarosa.messenger.recipients.Recipient;
import io.zonarosa.messenger.recipients.RecipientId;
import io.zonarosa.messenger.recipients.RecipientUtil;
import io.zonarosa.messenger.util.ZonaRosaPreferences;
import io.zonarosa.service.api.ZonaRosaServiceMessageSender;
import io.zonarosa.service.api.crypto.UntrustedIdentityException;
import io.zonarosa.service.api.messages.multidevice.ZonaRosaServiceSyncMessage;
import io.zonarosa.service.api.messages.multidevice.VerifiedMessage;
import io.zonarosa.service.api.push.ZonaRosaServiceAddress;
import io.zonarosa.service.api.push.exceptions.PushNetworkException;
import io.zonarosa.service.api.push.exceptions.ServerRejectedException;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class MultiDeviceVerifiedUpdateJob extends BaseJob {

  public static final String KEY = "MultiDeviceVerifiedUpdateJob";

  private static final String TAG = Log.tag(MultiDeviceVerifiedUpdateJob.class);

  private static final String KEY_DESTINATION     = "destination";
  private static final String KEY_IDENTITY_KEY    = "identity_key";
  private static final String KEY_VERIFIED_STATUS = "verified_status";
  private static final String KEY_TIMESTAMP       = "timestamp";

  private RecipientId    destination;
  private byte[]         identityKey;
  private VerifiedStatus verifiedStatus;
  private long           timestamp;

  public MultiDeviceVerifiedUpdateJob(@NonNull RecipientId destination, IdentityKey identityKey, VerifiedStatus verifiedStatus) {
    this(new Job.Parameters.Builder()
                           .addConstraint(NetworkConstraint.KEY)
                           .setQueue("__MULTI_DEVICE_VERIFIED_UPDATE__")
                           .setLifespan(TimeUnit.DAYS.toMillis(1))
                           .setMaxAttempts(Parameters.UNLIMITED)
                           .build(),
         destination,
         identityKey.serialize(),
         verifiedStatus,
         System.currentTimeMillis());
  }

  private MultiDeviceVerifiedUpdateJob(@NonNull Job.Parameters parameters,
                                       @NonNull RecipientId destination,
                                       @NonNull byte[] identityKey,
                                       @NonNull VerifiedStatus verifiedStatus,
                                       long timestamp)
  {
    super(parameters);

    this.destination    = destination;
    this.identityKey    = identityKey;
    this.verifiedStatus = verifiedStatus;
    this.timestamp      = timestamp;
  }

  @Override
  public @Nullable byte[] serialize() {
    return new JsonJobData.Builder().putString(KEY_DESTINATION, destination.serialize())
                                    .putString(KEY_IDENTITY_KEY, Base64.encodeWithPadding(identityKey))
                                    .putInt(KEY_VERIFIED_STATUS, verifiedStatus.toInt())
                                    .putLong(KEY_TIMESTAMP, timestamp)
                                    .serialize();
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

    try {
      if (!ZonaRosaStore.account().isMultiDevice()) {
        Log.i(TAG, "Not multi device...");
        return;
      }

      if (destination == null) {
        Log.w(TAG, "No destination...");
        return;
      }

      ZonaRosaServiceMessageSender messageSender = AppDependencies.getZonaRosaServiceMessageSender();
      Recipient                  recipient     = Recipient.resolved(destination);

      if (recipient.isUnregistered()) {
        Log.w(TAG, recipient.getId() + " not registered!");
        return;
      }

      VerifiedMessage.VerifiedState verifiedState   = getVerifiedState(verifiedStatus);
      ZonaRosaServiceAddress          verifiedAddress = RecipientUtil.toZonaRosaServiceAddress(context, recipient);
      VerifiedMessage               verifiedMessage = new VerifiedMessage(verifiedAddress, new IdentityKey(identityKey, 0), verifiedState, timestamp);

      messageSender.sendSyncMessage(ZonaRosaServiceSyncMessage.forVerified(verifiedMessage)
      );
    } catch (InvalidKeyException e) {
      throw new IOException(e);
    }
  }

  private VerifiedMessage.VerifiedState getVerifiedState(VerifiedStatus status) {
    VerifiedMessage.VerifiedState verifiedState;

    switch (status) {
      case DEFAULT:    verifiedState = VerifiedMessage.VerifiedState.DEFAULT;    break;
      case VERIFIED:   verifiedState = VerifiedMessage.VerifiedState.VERIFIED;   break;
      case UNVERIFIED: verifiedState = VerifiedMessage.VerifiedState.UNVERIFIED; break;
      default: throw new AssertionError("Unknown status: " + verifiedStatus);
    }

    return verifiedState;
  }

  @Override
  public boolean onShouldRetry(@NonNull Exception exception) {
    if (exception instanceof ServerRejectedException) return false;
    return exception instanceof PushNetworkException;
  }

  @Override
  public void onFailure() {

  }

  public static final class Factory implements Job.Factory<MultiDeviceVerifiedUpdateJob> {
    @Override
    public @NonNull MultiDeviceVerifiedUpdateJob create(@NonNull Parameters parameters, @Nullable byte[] serializedData) {
      JsonJobData data = JsonJobData.deserialize(serializedData);

      try {
        RecipientId    destination    = RecipientId.from(data.getString(KEY_DESTINATION));
        VerifiedStatus verifiedStatus = VerifiedStatus.forState(data.getInt(KEY_VERIFIED_STATUS));
        long           timestamp      = data.getLong(KEY_TIMESTAMP);
        byte[]         identityKey    = Base64.decode(data.getString(KEY_IDENTITY_KEY));

        return new MultiDeviceVerifiedUpdateJob(parameters, destination, identityKey, verifiedStatus, timestamp);
      } catch (IOException e) {
        throw new AssertionError(e);
      }
    }
  }
}
