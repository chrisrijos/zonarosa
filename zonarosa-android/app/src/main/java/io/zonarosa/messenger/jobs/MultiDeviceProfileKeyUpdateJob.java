package io.zonarosa.messenger.jobs;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.zonarosa.core.util.logging.Log;
import io.zonarosa.libzonarosa.zkgroup.profiles.ProfileKey;
import io.zonarosa.messenger.BuildConfig;
import io.zonarosa.messenger.crypto.ProfileKeyUtil;
import io.zonarosa.messenger.dependencies.AppDependencies;
import io.zonarosa.messenger.jobmanager.Job;
import io.zonarosa.messenger.jobmanager.impl.NetworkConstraint;
import io.zonarosa.messenger.keyvalue.ZonaRosaStore;
import io.zonarosa.messenger.net.NotPushRegisteredException;
import io.zonarosa.messenger.recipients.Recipient;
import io.zonarosa.messenger.util.RemoteConfig;
import io.zonarosa.service.api.ZonaRosaServiceMessageSender;
import io.zonarosa.service.api.crypto.UntrustedIdentityException;
import io.zonarosa.service.api.messages.ZonaRosaServiceAttachment;
import io.zonarosa.service.api.messages.ZonaRosaServiceAttachmentStream;
import io.zonarosa.service.api.messages.multidevice.ContactsMessage;
import io.zonarosa.service.api.messages.multidevice.DeviceContact;
import io.zonarosa.service.api.messages.multidevice.DeviceContactsOutputStream;
import io.zonarosa.service.api.messages.multidevice.ZonaRosaServiceSyncMessage;
import io.zonarosa.service.api.push.exceptions.PushNetworkException;
import io.zonarosa.service.api.push.exceptions.ServerRejectedException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class MultiDeviceProfileKeyUpdateJob extends BaseJob {

  public static String KEY = "MultiDeviceProfileKeyUpdateJob";

  private static final String TAG = Log.tag(MultiDeviceProfileKeyUpdateJob.class);

  public MultiDeviceProfileKeyUpdateJob() {
    this(new Job.Parameters.Builder()
                           .addConstraint(NetworkConstraint.KEY)
                           .setQueue("MultiDeviceProfileKeyUpdateJob")
                           .setLifespan(TimeUnit.DAYS.toMillis(1))
                           .setMaxAttempts(Parameters.UNLIMITED)
                           .build());
  }

  private MultiDeviceProfileKeyUpdateJob(@NonNull Job.Parameters parameters) {
    super(parameters);
  }

  @Override
  public @Nullable byte[] serialize() {
    return null;
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

    ByteArrayOutputStream      baos = new ByteArrayOutputStream();
    DeviceContactsOutputStream out  = new DeviceContactsOutputStream(baos, RemoteConfig.useBinaryId(), BuildConfig.USE_STRING_ID);

    out.write(new DeviceContact(Optional.ofNullable(ZonaRosaStore.account().getAci()),
                                Optional.ofNullable(ZonaRosaStore.account().getE164()),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty()));

    out.close();

    ZonaRosaServiceMessageSender    messageSender    = AppDependencies.getZonaRosaServiceMessageSender();
    ZonaRosaServiceAttachmentStream attachmentStream = ZonaRosaServiceAttachment.newStreamBuilder()
                                                                            .withStream(new ByteArrayInputStream(baos.toByteArray()))
                                                                            .withContentType("application/octet-stream")
                                                                            .withLength(baos.toByteArray().length)
                                                                            .withResumableUploadSpec(messageSender.getResumableUploadSpec())
                                                                            .build();

    ZonaRosaServiceSyncMessage syncMessage = ZonaRosaServiceSyncMessage.forContacts(new ContactsMessage(attachmentStream, false));

    messageSender.sendSyncMessage(syncMessage);
  }

  @Override
  public boolean onShouldRetry(@NonNull Exception exception) {
    if (exception instanceof ServerRejectedException) return false;
    if (exception instanceof PushNetworkException) return true;
    return false;
  }

  @Override
  public void onFailure() {
    Log.w(TAG, "Profile key sync failed!");
  }

  public static final class Factory implements Job.Factory<MultiDeviceProfileKeyUpdateJob> {
    @Override
    public @NonNull MultiDeviceProfileKeyUpdateJob create(@NonNull Parameters parameters, @Nullable byte[] serializedData) {
      return new MultiDeviceProfileKeyUpdateJob(parameters);
    }
  }
}
