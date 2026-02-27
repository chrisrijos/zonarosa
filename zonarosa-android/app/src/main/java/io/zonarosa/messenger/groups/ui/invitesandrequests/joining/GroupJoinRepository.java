package io.zonarosa.messenger.groups.ui.invitesandrequests.joining;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import io.zonarosa.core.util.concurrent.ZonaRosaExecutors;
import io.zonarosa.core.util.logging.Log;
import io.zonarosa.libzonarosa.zkgroup.VerificationFailedException;
import io.zonarosa.storageservice.storage.protos.groups.local.DecryptedGroupJoinInfo;
import io.zonarosa.messenger.groups.GroupChangeBusyException;
import io.zonarosa.messenger.groups.GroupChangeFailedException;
import io.zonarosa.messenger.groups.GroupManager;
import io.zonarosa.messenger.groups.MembershipNotSuitableForV2Exception;
import io.zonarosa.messenger.groups.v2.GroupInviteLinkUrl;
import io.zonarosa.messenger.jobs.AvatarGroupsV2DownloadJob;
import io.zonarosa.messenger.util.AsynchronousCallback;
import io.zonarosa.service.api.groupsv2.GroupLinkNotActiveException;
import io.zonarosa.service.internal.push.exceptions.GroupPatchNotAcceptedException;

import java.io.IOException;

final class GroupJoinRepository {

  private static final String TAG = Log.tag(GroupJoinRepository.class);

  private final Context            context;
  private final GroupInviteLinkUrl groupInviteLinkUrl;

  GroupJoinRepository(@NonNull Context context, @NonNull GroupInviteLinkUrl groupInviteLinkUrl) {
    this.context            = context;
    this.groupInviteLinkUrl = groupInviteLinkUrl;
  }

  void getGroupDetails(@NonNull AsynchronousCallback.WorkerThread<GroupDetails, FetchGroupDetailsError> callback) {
    ZonaRosaExecutors.UNBOUNDED.execute(() -> {
      try {
        callback.onComplete(getGroupDetails());
      } catch (IOException e) {
        callback.onError(FetchGroupDetailsError.NetworkError);
      } catch (GroupLinkNotActiveException e) {
        callback.onError(e.getReason() == GroupLinkNotActiveException.Reason.BANNED ? FetchGroupDetailsError.BannedFromGroup : FetchGroupDetailsError.GroupLinkNotActive);
      } catch (VerificationFailedException e) {
        callback.onError(FetchGroupDetailsError.GroupLinkNotActive);
      }
    });
  }

  void joinGroup(@NonNull GroupDetails groupDetails,
                 @NonNull AsynchronousCallback.WorkerThread<JoinGroupSuccess, JoinGroupError> callback)
  {
    ZonaRosaExecutors.UNBOUNDED.execute(() -> {
      try {
        GroupManager.GroupActionResult groupActionResult = GroupManager.joinGroup(context,
                                                                                  groupInviteLinkUrl.getGroupMasterKey(),
                                                                                  groupInviteLinkUrl.getPassword(),
                                                                                  groupDetails.getJoinInfo(),
                                                                                  groupDetails.getAvatarBytes());

        callback.onComplete(new JoinGroupSuccess(groupActionResult.getGroupRecipient(), groupActionResult.getThreadId()));
      } catch (IOException e) {
        Log.w(TAG, "Network error", e);
        callback.onError(JoinGroupError.NETWORK_ERROR);
      } catch (GroupChangeBusyException e) {
        Log.w(TAG, "Change error", e);
        callback.onError(JoinGroupError.BUSY);
      } catch (GroupLinkNotActiveException e) {
        Log.w(TAG, "Inactive group error", e);
        callback.onError(e.getReason() == GroupLinkNotActiveException.Reason.BANNED ? JoinGroupError.BANNED : JoinGroupError.GROUP_LINK_NOT_ACTIVE);
      } catch (MembershipNotSuitableForV2Exception e) {
        Log.w(TAG, "Membership not suitable", e);
        callback.onError(JoinGroupError.FAILED);
      } catch (GroupChangeFailedException e) {
        Log.w(TAG, "Group change failed", e);
        JoinGroupError error = JoinGroupError.FAILED;
        if (e.getCause() instanceof GroupPatchNotAcceptedException) {
          String message = e.getCause().getMessage();
          if (message != null && message.contains("group size cannot exceed")) {
            error = JoinGroupError.LIMIT_REACHED;
          }
        }
        callback.onError(error);
      }
    });
  }

  @WorkerThread
  private @NonNull GroupDetails getGroupDetails()
      throws VerificationFailedException, IOException, GroupLinkNotActiveException
  {
    DecryptedGroupJoinInfo joinInfo = GroupManager.getGroupJoinInfoFromServer(context,
                                                                              groupInviteLinkUrl.getGroupMasterKey(),
                                                                              groupInviteLinkUrl.getPassword());

    byte[] avatarBytes = tryGetAvatarBytes(joinInfo);

    return new GroupDetails(joinInfo, avatarBytes);
  }

  private @Nullable byte[] tryGetAvatarBytes(@NonNull DecryptedGroupJoinInfo joinInfo) {
    try {
      return AvatarGroupsV2DownloadJob.downloadGroupAvatarBytes(context, groupInviteLinkUrl.getGroupMasterKey(), joinInfo.avatar);
    } catch (IOException e) {
      Log.w(TAG, "Failed to get group avatar", e);
      return null;
    }
  }
}
