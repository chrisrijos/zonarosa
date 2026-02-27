package io.zonarosa.messenger.groups.ui.invitesandrequests.requesting;

import android.content.Context;

import androidx.annotation.NonNull;

import io.zonarosa.core.util.concurrent.ZonaRosaExecutors;
import io.zonarosa.core.util.logging.Log;
import io.zonarosa.messenger.groups.GroupChangeException;
import io.zonarosa.messenger.groups.GroupId;
import io.zonarosa.messenger.groups.GroupManager;
import io.zonarosa.messenger.groups.ui.GroupChangeFailureReason;
import io.zonarosa.messenger.recipients.Recipient;
import io.zonarosa.messenger.util.AsynchronousCallback;

import java.io.IOException;
import java.util.Collections;

/**
 * Repository for modifying the requesting members on a single group.
 */
final class RequestingMemberRepository {

  private static final String TAG = Log.tag(RequestingMemberRepository.class);

  private final Context    context;
  private final GroupId.V2 groupId;

  RequestingMemberRepository(@NonNull Context context, @NonNull GroupId.V2 groupId) {
    this.context = context.getApplicationContext();
    this.groupId = groupId;
  }

  void approveRequest(@NonNull Recipient recipient,
                      @NonNull AsynchronousCallback.WorkerThread<Void, GroupChangeFailureReason> callback)
  {
    ZonaRosaExecutors.UNBOUNDED.execute(() -> {
      try {
        GroupManager.approveRequests(context, groupId, Collections.singleton(recipient.getId()));
        callback.onComplete(null);
      } catch (GroupChangeException | IOException e) {
        Log.w(TAG, e);
        callback.onError(GroupChangeFailureReason.fromException(e));
      }
    });
  }

  void denyRequest(@NonNull Recipient recipient,
                   @NonNull AsynchronousCallback.WorkerThread<Void, GroupChangeFailureReason> callback)
  {
    ZonaRosaExecutors.UNBOUNDED.execute(() -> {
      try {
        GroupManager.denyRequests(context, groupId, Collections.singleton(recipient.getId()));
        callback.onComplete(null);
      } catch (GroupChangeException | IOException e) {
        Log.w(TAG, e);
        callback.onError(GroupChangeFailureReason.fromException(e));
      }
    });
  }
}
