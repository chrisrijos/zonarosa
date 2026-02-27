package io.zonarosa.messenger.groups.ui.invitesandrequests.invite;

import android.content.Context;

import androidx.annotation.NonNull;

import io.zonarosa.core.util.concurrent.ZonaRosaExecutors;
import io.zonarosa.messenger.groups.GroupChangeBusyException;
import io.zonarosa.messenger.groups.GroupChangeFailedException;
import io.zonarosa.messenger.groups.GroupId;
import io.zonarosa.messenger.groups.GroupInsufficientRightsException;
import io.zonarosa.messenger.groups.GroupManager;
import io.zonarosa.messenger.groups.GroupNotAMemberException;
import io.zonarosa.messenger.groups.v2.GroupInviteLinkUrl;
import io.zonarosa.messenger.util.AsynchronousCallback;

import java.io.IOException;

final class GroupLinkInviteRepository {

  private final Context    context;
  private final GroupId.V2 groupId;

  GroupLinkInviteRepository(@NonNull Context context, @NonNull GroupId.V2 groupId) {
    this.context = context;
    this.groupId = groupId;
  }

  void enableGroupInviteLink(boolean requireMemberApproval, @NonNull AsynchronousCallback.WorkerThread<GroupInviteLinkUrl, EnableInviteLinkError> callback) {
    ZonaRosaExecutors.UNBOUNDED.execute(() -> {
      try {
        GroupInviteLinkUrl groupInviteLinkUrl = GroupManager.setGroupLinkEnabledState(context,
                                                                                      groupId,
                                                                                      requireMemberApproval ? GroupManager.GroupLinkState.ENABLED_WITH_APPROVAL
                                                                                                            : GroupManager.GroupLinkState.ENABLED);

        if (groupInviteLinkUrl == null) {
          throw new AssertionError();
        }

        callback.onComplete(groupInviteLinkUrl);
      } catch (IOException e) {
        callback.onError(EnableInviteLinkError.NETWORK_ERROR);
      } catch (GroupChangeBusyException e) {
        callback.onError(EnableInviteLinkError.BUSY);
      } catch (GroupChangeFailedException e) {
        callback.onError(EnableInviteLinkError.FAILED);
      } catch (GroupInsufficientRightsException e) {
        callback.onError(EnableInviteLinkError.INSUFFICIENT_RIGHTS);
      } catch (GroupNotAMemberException e) {
        callback.onError(EnableInviteLinkError.NOT_IN_GROUP);
      }
    });
  }
}
