package io.zonarosa.messenger.profiles.edit;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.core.util.Consumer;

import io.zonarosa.core.util.StreamUtil;
import io.zonarosa.core.util.logging.Log;
import io.zonarosa.messenger.conversation.colors.AvatarColor;
import io.zonarosa.messenger.database.ZonaRosaDatabase;
import io.zonarosa.messenger.database.model.GroupRecord;
import io.zonarosa.messenger.groups.GroupChangeException;
import io.zonarosa.messenger.groups.GroupId;
import io.zonarosa.messenger.groups.GroupManager;
import io.zonarosa.messenger.profiles.AvatarHelper;
import io.zonarosa.messenger.profiles.ProfileName;
import io.zonarosa.messenger.recipients.Recipient;
import io.zonarosa.messenger.recipients.RecipientId;
import io.zonarosa.core.util.concurrent.SimpleTask;

import java.io.IOException;
import java.util.Optional;

class EditGroupProfileRepository implements EditProfileRepository {

  private static final String TAG = Log.tag(EditGroupProfileRepository.class);

  private final Context context;
  private final GroupId groupId;

  EditGroupProfileRepository(@NonNull Context context, @NonNull GroupId groupId) {
    this.context = context.getApplicationContext();
    this.groupId = groupId;
  }

  @Override
  public void getCurrentAvatarColor(@NonNull Consumer<AvatarColor> avatarColorConsumer) {
    SimpleTask.run(() -> Recipient.resolved(getRecipientId()).getAvatarColor(), avatarColorConsumer::accept);
  }

  @Override
  public void getCurrentProfileName(@NonNull Consumer<ProfileName> profileNameConsumer) {
    profileNameConsumer.accept(ProfileName.EMPTY);
  }

  @Override
  public void getCurrentAvatar(@NonNull Consumer<byte[]> avatarConsumer) {
    SimpleTask.run(() -> {
      final RecipientId recipientId = getRecipientId();

      if (AvatarHelper.hasAvatar(context, recipientId)) {
        try {
          return StreamUtil.readFully(AvatarHelper.getAvatar(context, recipientId));
        } catch (IOException e) {
          Log.w(TAG, e);
          return null;
        }
      } else {
        return null;
      }
    }, avatarConsumer::accept);
  }

  @Override
  public void getCurrentDisplayName(@NonNull Consumer<String> displayNameConsumer) {
    SimpleTask.run(() -> Recipient.resolved(getRecipientId()).getDisplayName(context), displayNameConsumer::accept);
  }

  @Override
  public void getCurrentName(@NonNull Consumer<String> nameConsumer) {
    SimpleTask.run(() -> {
      RecipientId recipientId = getRecipientId();
      Recipient   recipient   = Recipient.resolved(recipientId);

      return ZonaRosaDatabase.groups()
                           .getGroup(recipientId)
                           .map(groupRecord -> {
                              String title = groupRecord.getTitle();
                              return title == null ? "" : title;
                            })
                           .orElseGet(() -> recipient.getGroupName(context));
    }, nameConsumer::accept);
  }

  @Override
  public void getCurrentDescription(@NonNull Consumer<String> descriptionConsumer) {
    SimpleTask.run(() -> {
      RecipientId recipientId = getRecipientId();

      return ZonaRosaDatabase.groups()
                           .getGroup(recipientId)
                           .map(GroupRecord::getDescription)
                           .orElse("");
    }, descriptionConsumer::accept);
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
      try {
        GroupManager.updateGroupDetails(context, groupId, avatar, avatarChanged, displayName, displayNameChanged, description, descriptionChanged);

        return UploadResult.SUCCESS;
      } catch (GroupChangeException | IOException e) {
        return UploadResult.ERROR_IO;
      }

    }, uploadResultConsumer::accept);
  }

  @Override
  public void getCurrentUsername(@NonNull Consumer<Optional<String>> callback) {
    callback.accept(Optional.empty());
  }

  @WorkerThread
  private RecipientId getRecipientId() {
    return ZonaRosaDatabase.recipients().getByGroupId(groupId)
                         .orElseThrow(() -> new AssertionError("Recipient ID for Group ID does not exist."));
  }
}
