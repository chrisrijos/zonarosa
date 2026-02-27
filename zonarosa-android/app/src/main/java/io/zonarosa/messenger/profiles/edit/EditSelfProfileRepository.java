package io.zonarosa.messenger.profiles.edit;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import io.zonarosa.core.util.StreamUtil;
import io.zonarosa.core.util.concurrent.ListenableFuture;
import io.zonarosa.core.util.concurrent.SimpleTask;
import io.zonarosa.core.util.logging.Log;
import io.zonarosa.messenger.conversation.colors.AvatarColor;
import io.zonarosa.messenger.database.ZonaRosaDatabase;
import io.zonarosa.messenger.dependencies.AppDependencies;
import io.zonarosa.messenger.jobs.MultiDeviceProfileContentUpdateJob;
import io.zonarosa.messenger.jobs.MultiDeviceProfileKeyUpdateJob;
import io.zonarosa.messenger.jobs.ProfileUploadJob;
import io.zonarosa.messenger.keyvalue.ZonaRosaStore;
import io.zonarosa.messenger.profiles.AvatarHelper;
import io.zonarosa.messenger.profiles.ProfileMediaConstraints;
import io.zonarosa.messenger.profiles.ProfileName;
import io.zonarosa.messenger.profiles.SystemProfileUtil;
import io.zonarosa.messenger.recipients.Recipient;
import io.zonarosa.messenger.recipients.RecipientId;
import io.zonarosa.messenger.registration.util.RegistrationUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class EditSelfProfileRepository implements EditProfileRepository {

  private static final String TAG = Log.tag(EditSelfProfileRepository.class);

  private final Context context;
  private final boolean excludeSystem;

  EditSelfProfileRepository(@NonNull Context context, boolean excludeSystem) {
    this.context        = context.getApplicationContext();
    this.excludeSystem  = excludeSystem;
  }

  @Override
  public void getCurrentAvatarColor(@NonNull Consumer<AvatarColor> avatarColorConsumer) {
    SimpleTask.run(() -> Recipient.self().getAvatarColor(), avatarColorConsumer::accept);
  }

  @Override
  public void getCurrentProfileName(@NonNull Consumer<ProfileName> profileNameConsumer) {
    ProfileName storedProfileName = Recipient.self().getProfileName();
    if (!storedProfileName.isEmpty()) {
      profileNameConsumer.accept(storedProfileName);
    } else if (!excludeSystem) {
      SystemProfileUtil.getSystemProfileName(context).addListener(new ListenableFuture.Listener<String>() {
        @Override
        public void onSuccess(String result) {
          if (!TextUtils.isEmpty(result)) {
            profileNameConsumer.accept(ProfileName.fromSerialized(result));
          } else {
            profileNameConsumer.accept(storedProfileName);
          }
        }

        @Override
        public void onFailure(ExecutionException e) {
          Log.w(TAG, e);
          profileNameConsumer.accept(storedProfileName);
        }
      });
    } else {
      profileNameConsumer.accept(storedProfileName);
    }
  }

  @Override
  public void getCurrentAvatar(@NonNull Consumer<byte[]> avatarConsumer) {
    RecipientId selfId = Recipient.self().getId();

    if (AvatarHelper.hasAvatar(context, selfId)) {
      SimpleTask.run(() -> {
        try {
          return StreamUtil.readFully(AvatarHelper.getAvatar(context, selfId));
        } catch (IOException e) {
          Log.w(TAG, e);
          return null;
        }
      }, avatarConsumer::accept);
    } else if (!excludeSystem) {
      SystemProfileUtil.getSystemProfileAvatar(context, new ProfileMediaConstraints()).addListener(new ListenableFuture.Listener<byte[]>() {
        @Override
        public void onSuccess(byte[] result) {
          avatarConsumer.accept(result);
        }

        @Override
        public void onFailure(ExecutionException e) {
          Log.w(TAG, e);
          avatarConsumer.accept(null);
        }
      });
    }
  }

  @Override
  public void getCurrentDisplayName(@NonNull Consumer<String> displayNameConsumer) {
    displayNameConsumer.accept("");
  }

  @Override
  public void getCurrentName(@NonNull Consumer<String> nameConsumer) {
    nameConsumer.accept("");
  }

  @Override public void getCurrentDescription(@NonNull Consumer<String> descriptionConsumer) {
    descriptionConsumer.accept("");
  }

  @Override
  public void uploadProfile(@NonNull ProfileName profileName,
                            @NonNull String displayName,
                            boolean displayNameChanged,
                            @NonNull String description,
                            boolean descriptionChanged,
                            @Nullable byte[] avatar,
                            boolean avatarChanged,
                            @NonNull Consumer<UploadResult> uploadResultConsumer)
  {
    SimpleTask.run(() -> {
      ZonaRosaDatabase.recipients().setProfileName(Recipient.self().getId(), profileName);

      if (avatarChanged) {
        try {
          AvatarHelper.setAvatar(context, Recipient.self().getId(), avatar != null ? new ByteArrayInputStream(avatar) : null);
        } catch (IOException e) {
          return UploadResult.ERROR_IO;
        }
      }

      AppDependencies.getJobManager()
                     .startChain(new ProfileUploadJob())
                     .then(Arrays.asList(new MultiDeviceProfileKeyUpdateJob(), new MultiDeviceProfileContentUpdateJob()))
                     .enqueue();

      RegistrationUtil.maybeMarkRegistrationComplete();

      if (avatar != null) {
        ZonaRosaStore.misc().setHasEverHadAnAvatar(true);
      }

      return UploadResult.SUCCESS;
    }, uploadResultConsumer::accept);
  }

  @Override
  public void getCurrentUsername(@NonNull Consumer<Optional<String>> callback) {
    callback.accept(Optional.ofNullable(ZonaRosaStore.account().getUsername()));
  }
}
