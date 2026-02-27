package io.zonarosa.messenger.jobs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.zonarosa.core.util.logging.Log;
import io.zonarosa.libzonarosa.protocol.InvalidMessageException;
import io.zonarosa.messenger.database.GroupTable;
import io.zonarosa.messenger.database.model.GroupRecord;
import io.zonarosa.messenger.database.ZonaRosaDatabase;
import io.zonarosa.messenger.dependencies.AppDependencies;
import io.zonarosa.messenger.groups.GroupId;
import io.zonarosa.messenger.jobmanager.JsonJobData;
import io.zonarosa.messenger.jobmanager.Job;
import io.zonarosa.messenger.jobmanager.impl.NetworkConstraint;
import io.zonarosa.messenger.profiles.AvatarHelper;
import io.zonarosa.core.util.Hex;
import io.zonarosa.service.api.ZonaRosaServiceMessageReceiver;
import io.zonarosa.service.api.crypto.AttachmentCipherInputStream;
import io.zonarosa.service.api.crypto.AttachmentCipherInputStream.IntegrityCheck;
import io.zonarosa.service.api.messages.ZonaRosaServiceAttachmentPointer;
import io.zonarosa.service.api.messages.ZonaRosaServiceAttachmentRemoteId;
import io.zonarosa.service.api.push.exceptions.MissingConfigurationException;
import io.zonarosa.service.api.push.exceptions.NonSuccessfulResponseCodeException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public final class AvatarGroupsV1DownloadJob extends BaseJob {

  public static final String KEY = "AvatarDownloadJob";

  private static final String TAG = Log.tag(AvatarGroupsV1DownloadJob.class);

  private static final String KEY_GROUP_ID = "group_id";

  @NonNull private final GroupId.V1 groupId;

  public AvatarGroupsV1DownloadJob(@NonNull GroupId.V1 groupId) {
    this(new Job.Parameters.Builder()
                           .addConstraint(NetworkConstraint.KEY)
                           .setMaxAttempts(10)
                           .build(),
         groupId);
  }

  private AvatarGroupsV1DownloadJob(@NonNull Job.Parameters parameters, @NonNull GroupId.V1 groupId) {
    super(parameters);
    this.groupId = groupId;
  }

  @Override
  public @Nullable byte[] serialize() {
    return new JsonJobData.Builder().putString(KEY_GROUP_ID, groupId.toString()).serialize();
  }

  @Override
  public @NonNull String getFactoryKey() {
    return KEY;
  }

  @Override
  public void onRun() throws IOException {
    GroupTable            database = ZonaRosaDatabase.groups();
    Optional<GroupRecord> record   = database.getGroup(groupId);
    File                  attachment = null;

    try {
      if (record.isPresent()) {
        long             avatarId    = record.get().getAvatarId();
        String           contentType = record.get().getAvatarContentType();
        byte[]           key         = record.get().getAvatarKey();
        Optional<byte[]> digest      = Optional.ofNullable(record.get().getAvatarDigest());
        Optional<String> fileName    = Optional.empty();

        if (avatarId == -1 || key == null) {
          return;
        }

        if (digest.isPresent()) {
          Log.i(TAG, "Downloading group avatar with digest: " + Hex.toString(digest.get()));
        }

        attachment = File.createTempFile("avatar", "tmp", context.getCacheDir());
        attachment.deleteOnExit();


        ZonaRosaServiceMessageReceiver   receiver = AppDependencies.getZonaRosaServiceMessageReceiver();
        ZonaRosaServiceAttachmentPointer pointer  = new ZonaRosaServiceAttachmentPointer(0, new ZonaRosaServiceAttachmentRemoteId.V2(avatarId), contentType, key, Optional.of(0), Optional.empty(), 0, 0, digest, Optional.empty(), 0, fileName, false, false, false, Optional.empty(), Optional.empty(), System.currentTimeMillis(), null);

        if (pointer.getDigest().isEmpty()) {
          throw new InvalidMessageException("Missing digest!");
        }

        IntegrityCheck integrityCheck = IntegrityCheck.forEncryptedDigest(pointer.getDigest().get());
        InputStream    inputStream    = receiver.retrieveAttachment(pointer, attachment, AvatarHelper.AVATAR_DOWNLOAD_FAILSAFE_MAX_SIZE, integrityCheck);

        AvatarHelper.setAvatar(context, record.get().getRecipientId(), inputStream);
        ZonaRosaDatabase.groups().onAvatarUpdated(groupId, true);

        inputStream.close();
      }
    } catch (NonSuccessfulResponseCodeException | InvalidMessageException | MissingConfigurationException e) {
      Log.w(TAG, e);
    } finally {
      if (attachment != null)
        attachment.delete();
    }
  }

  @Override
  public void onFailure() {}

  @Override
  public boolean onShouldRetry(@NonNull Exception exception) {
    if (exception instanceof IOException) return true;
    return false;
  }

  public static final class Factory implements Job.Factory<AvatarGroupsV1DownloadJob> {
    @Override
    public @NonNull AvatarGroupsV1DownloadJob create(@NonNull Parameters parameters, @Nullable byte[] serializedData) {
      JsonJobData data = JsonJobData.deserialize(serializedData);
      return new AvatarGroupsV1DownloadJob(parameters, GroupId.parseOrThrow(data.getString(KEY_GROUP_ID)).requireV1());
    }
  }
}
