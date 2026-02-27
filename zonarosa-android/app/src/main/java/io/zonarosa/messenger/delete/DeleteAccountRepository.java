package io.zonarosa.messenger.delete;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;

import com.annimon.stream.Stream;
import com.google.i18n.phonenumbers.PhoneNumberUtil;

import io.zonarosa.core.util.E164Util;
import io.zonarosa.core.util.concurrent.ZonaRosaExecutors;
import io.zonarosa.core.util.logging.Log;
import io.zonarosa.messenger.components.settings.app.subscription.InAppPaymentsRepository;
import io.zonarosa.messenger.database.GroupTable;
import io.zonarosa.messenger.database.ZonaRosaDatabase;
import io.zonarosa.messenger.database.model.GroupRecord;
import io.zonarosa.messenger.database.model.InAppPaymentSubscriberRecord;
import io.zonarosa.messenger.dependencies.AppDependencies;
import io.zonarosa.messenger.groups.GroupManager;
import io.zonarosa.messenger.net.ZonaRosaNetwork;
import io.zonarosa.messenger.util.ServiceUtil;
import io.zonarosa.service.api.NetworkResultUtil;
import io.zonarosa.service.api.push.exceptions.NonSuccessfulResponseCodeException;
import io.zonarosa.service.internal.EmptyResponse;
import io.zonarosa.service.internal.ServiceResponse;

import java.io.IOException;

class DeleteAccountRepository {
  private static final String TAG = Log.tag(DeleteAccountRepository.class);

  @NonNull String getRegionDisplayName(@NonNull String region) {
    return E164Util.getRegionDisplayName(region).orElse("");
  }

  int getRegionCountryCode(@NonNull String region) {
    return PhoneNumberUtil.getInstance().getCountryCodeForRegion(region);
  }

  void deleteAccount(@NonNull Consumer<DeleteAccountEvent> onDeleteAccountEvent) {
    ZonaRosaExecutors.BOUNDED.execute(() -> {
      if (InAppPaymentsRepository.getSubscriber(InAppPaymentSubscriberRecord.Type.DONATION) != null) {
        Log.i(TAG, "deleteAccount: attempting to cancel subscription");
        onDeleteAccountEvent.accept(DeleteAccountEvent.CancelingSubscription.INSTANCE);

        InAppPaymentSubscriberRecord subscriber = InAppPaymentsRepository.requireSubscriber(InAppPaymentSubscriberRecord.Type.DONATION);
        ServiceResponse<EmptyResponse> cancelSubscriptionResponse = AppDependencies.getDonationsService()
                                                                                   .cancelSubscription(subscriber.getSubscriberId());

        if (cancelSubscriptionResponse.getExecutionError().isPresent()) {
          Log.w(TAG, "deleteAccount: failed attempt to cancel subscription");
          onDeleteAccountEvent.accept(DeleteAccountEvent.CancelSubscriptionFailed.INSTANCE);
          return;
        }

        switch (cancelSubscriptionResponse.getStatus()) {
          case 404:
            Log.i(TAG, "deleteAccount: subscription does not exist. Continuing deletion...");
            break;
          case 200:
            Log.i(TAG, "deleteAccount: successfully cancelled subscription. Continuing deletion...");
            break;
          default:
            Log.w(TAG, "deleteAccount: an unexpected error occurred. " + cancelSubscriptionResponse.getStatus());
            onDeleteAccountEvent.accept(DeleteAccountEvent.CancelSubscriptionFailed.INSTANCE);
            return;
        }
      }

      Log.i(TAG, "deleteAccount: attempting to leave groups...");

      int groupsLeft = 0;
      try (GroupTable.Reader groups = ZonaRosaDatabase.groups().getGroups()) {
        GroupRecord groupRecord = groups.getNext();
        onDeleteAccountEvent.accept(new DeleteAccountEvent.LeaveGroupsProgress(groups.getCount(), 0));
        Log.i(TAG, "deleteAccount: found " + groups.getCount() + " groups to leave.");

        while (groupRecord != null) {
          if (groupRecord.getId().isPush() && groupRecord.isActive()) {
            if (!groupRecord.isV1Group()) {
              GroupManager.leaveGroup(AppDependencies.getApplication(), groupRecord.getId().requirePush(), true);
            }
            onDeleteAccountEvent.accept(new DeleteAccountEvent.LeaveGroupsProgress(groups.getCount(), ++groupsLeft));
          }

          groupRecord = groups.getNext();
        }

        onDeleteAccountEvent.accept(DeleteAccountEvent.LeaveGroupsFinished.INSTANCE);
      } catch (Exception e) {
        Log.w(TAG, "deleteAccount: failed to leave groups", e);
        onDeleteAccountEvent.accept(DeleteAccountEvent.LeaveGroupsFailed.INSTANCE);
        return;
      }

      Log.i(TAG, "deleteAccount: successfully left all groups.");
      Log.i(TAG, "deleteAccount: attempting to delete account from server...");

      try {
        NetworkResultUtil.toBasicLegacy(ZonaRosaNetwork.account().deleteAccount());
      } catch (IOException e) {
        if (e instanceof NonSuccessfulResponseCodeException && ((NonSuccessfulResponseCodeException) e).code == 4401) {
          Log.i(TAG, "deleteAccount: WebSocket closed with expected status after delete account, moving forward as delete was successful");
        } else {
          Log.w(TAG, "deleteAccount: failed to delete account from zonarosa service, bail", e);
          onDeleteAccountEvent.accept(DeleteAccountEvent.ServerDeletionFailed.INSTANCE);
          return;
        }
      }

      Log.i(TAG, "deleteAccount: successfully removed account from server");
      Log.i(TAG, "deleteAccount: attempting to delete user data and close process...");

      if (!ServiceUtil.getActivityManager(AppDependencies.getApplication()).clearApplicationUserData()) {
        Log.w(TAG, "deleteAccount: failed to delete user data");
        onDeleteAccountEvent.accept(DeleteAccountEvent.LocalDataDeletionFailed.INSTANCE);
      }
    });
  }
}
