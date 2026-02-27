package io.zonarosa.messenger.recipients.ui.sharablegrouplink;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import io.zonarosa.core.util.concurrent.ZonaRosaExecutors;
import io.zonarosa.storageservice.storage.protos.groups.AccessControl;
import io.zonarosa.messenger.database.ZonaRosaDatabase;
import io.zonarosa.messenger.groups.GroupChangeBusyException;
import io.zonarosa.messenger.groups.GroupChangeFailedException;
import io.zonarosa.messenger.groups.GroupId;
import io.zonarosa.messenger.groups.GroupInsufficientRightsException;
import io.zonarosa.messenger.groups.GroupManager;
import io.zonarosa.messenger.groups.GroupNotAMemberException;
import io.zonarosa.messenger.groups.ui.GroupChangeFailureReason;
import io.zonarosa.messenger.util.AsynchronousCallback;

import java.io.IOException;

final class ShareableGroupLinkRepository {

  private final Context    context;
  private final GroupId.V2 groupId;

  ShareableGroupLinkRepository(@NonNull Context context, @NonNull GroupId.V2 groupId) {
    this.context = context;
    this.groupId = groupId;
  }

  void cycleGroupLinkPassword(@NonNull AsynchronousCallback.WorkerThread<Void, GroupChangeFailureReason> callback) {
    ZonaRosaExecutors.UNBOUNDED.execute(() -> {
      try {
        GroupManager.cycleGroupLinkPassword(context, groupId);
        callback.onComplete(null);
      } catch (GroupNotAMemberException | GroupChangeFailedException | GroupInsufficientRightsException | IOException | GroupChangeBusyException e) {
        callback.onError(GroupChangeFailureReason.fromException(e));
      }
    });
  }

  void toggleGroupLinkEnabled(@NonNull AsynchronousCallback.WorkerThread<Void, GroupChangeFailureReason> callback) {
    setGroupLinkEnabledState(toggleGroupLinkState(true, false), callback);
  }

  void toggleGroupLinkApprovalRequired(@NonNull AsynchronousCallback.WorkerThread<Void, GroupChangeFailureReason> callback) {
    setGroupLinkEnabledState(toggleGroupLinkState(false, true), callback);
  }

  private void setGroupLinkEnabledState(@NonNull GroupManager.GroupLinkState state,
                                        @NonNull AsynchronousCallback.WorkerThread<Void, GroupChangeFailureReason> callback)
  {
    ZonaRosaExecutors.UNBOUNDED.execute(() -> {
      try {
        GroupManager.setGroupLinkEnabledState(context, groupId, state);
        callback.onComplete(null);
      } catch (GroupNotAMemberException | GroupChangeFailedException | GroupInsufficientRightsException | IOException | GroupChangeBusyException e) {
        callback.onError(GroupChangeFailureReason.fromException(e));
      }
    });
  }

  @WorkerThread
  private GroupManager.GroupLinkState toggleGroupLinkState(boolean toggleEnabled, boolean toggleApprovalNeeded) {
    AccessControl.AccessRequired currentState = ZonaRosaDatabase.groups()
                                                              .getGroup(groupId)
                                                              .get()
                                                              .requireV2GroupProperties()
                                                              .getDecryptedGroup()
                                                              .accessControl
                                                              .addFromInviteLink;

    boolean enabled;
    boolean approvalNeeded;

    switch (currentState) {
      case UNKNOWN:
      case UNSATISFIABLE:
      case MEMBER:
        enabled        = false;
        approvalNeeded = false;
        break;
      case ANY:
        enabled        = true;
        approvalNeeded = false;
        break;
      case ADMINISTRATOR:
        enabled        = true;
        approvalNeeded = true;
        break;
      default: throw new AssertionError();
    }

    if (toggleApprovalNeeded) {
      approvalNeeded = !approvalNeeded;
    }

    if (toggleEnabled) {
      enabled = !enabled;
    }

    if (approvalNeeded && enabled) {
      return GroupManager.GroupLinkState.ENABLED_WITH_APPROVAL;
    } else {
      if (enabled) {
        return GroupManager.GroupLinkState.ENABLED;
      }
    }

    return GroupManager.GroupLinkState.DISABLED;
  }
}
