package io.zonarosa.messenger.logsubmit;

import android.content.Context;

import androidx.annotation.NonNull;

import io.zonarosa.donations.InAppPaymentType;
import io.zonarosa.messenger.components.settings.app.subscription.InAppPaymentsRepository;
import io.zonarosa.messenger.database.InAppPaymentTable;
import io.zonarosa.messenger.database.ZonaRosaDatabase;
import io.zonarosa.messenger.database.model.InAppPaymentSubscriberRecord;
import io.zonarosa.messenger.database.model.databaseprotos.InAppPaymentData;
import io.zonarosa.messenger.keyvalue.ZonaRosaStore;
import io.zonarosa.messenger.recipients.Recipient;

final class LogSectionBadges implements LogSection {

  @Override
  public @NonNull String getTitle() {
    return "BADGES";
  }

  @Override
  public @NonNull CharSequence getContent(@NonNull Context context) {
    if (!ZonaRosaStore.account().isRegistered()) {
      return "Unregistered";
    }

    if (ZonaRosaStore.account().getE164() == null || ZonaRosaStore.account().getAci() == null) {
      return "Self not yet available!";
    }

    InAppPaymentTable.InAppPayment latestRecurringDonation = ZonaRosaDatabase.inAppPayments().getLatestInAppPaymentByType(InAppPaymentType.RECURRING_DONATION);

    if (latestRecurringDonation != null) {
      return new StringBuilder().append("Badge Count                       : ").append(Recipient.self().getBadges().size()).append("\n")
                                .append("ExpiredBadge                      : ").append(ZonaRosaStore.inAppPayments().getExpiredBadge() != null).append("\n")
                                .append("LastKeepAliveLaunchTime           : ").append(ZonaRosaStore.inAppPayments().getLastKeepAliveLaunchTime()).append("\n")
                                .append("LastEndOfPeriod                   : ").append(ZonaRosaStore.inAppPayments().getLastEndOfPeriod()).append("\n")
                                .append("InAppPayment.State                : ").append(latestRecurringDonation.getState()).append("\n")
                                .append("InAppPayment.EndOfPeriod          : ").append(latestRecurringDonation.getEndOfPeriodSeconds()).append("\n")
                                .append("InAppPaymentData.PaymentMethodType: ").append(getPaymentMethod(latestRecurringDonation.getData())).append("\n")
                                .append("InAppPaymentData.RedemptionState  : ").append(getRedemptionStage(latestRecurringDonation.getData())).append("\n")
                                .append("InAppPaymentData.Error            : ").append(getError(latestRecurringDonation.getData())).append("\n")
                                .append("InAppPaymentData.Cancellation     : ").append(getCancellation(latestRecurringDonation.getData())).append("\n")
                                .append("DisplayBadgesOnProfile            : ").append(ZonaRosaStore.inAppPayments().getDisplayBadgesOnProfile()).append("\n")
                                .append("ShouldCancelBeforeNextAttempt     : ").append(InAppPaymentsRepository.getShouldCancelSubscriptionBeforeNextSubscribeAttempt(InAppPaymentSubscriberRecord.Type.DONATION)).append("\n")
                                .append("IsUserManuallyCancelledDonation   : ").append(ZonaRosaStore.inAppPayments().isDonationSubscriptionManuallyCancelled()).append("\n");

    } else {
      return new StringBuilder().append("Badge Count                             : ").append(Recipient.self().getBadges().size()).append("\n")
                                .append("ExpiredBadge                            : ").append(ZonaRosaStore.inAppPayments().getExpiredBadge() != null).append("\n")
                                .append("LastKeepAliveLaunchTime                 : ").append(ZonaRosaStore.inAppPayments().getLastKeepAliveLaunchTime()).append("\n")
                                .append("LastEndOfPeriod                         : ").append(ZonaRosaStore.inAppPayments().getLastEndOfPeriod()).append("\n")
                                .append("IsUserManuallyCancelledDonation         : ").append(ZonaRosaStore.inAppPayments().isDonationSubscriptionManuallyCancelled()).append("\n")
                                .append("DisplayBadgesOnProfile                  : ").append(ZonaRosaStore.inAppPayments().getDisplayBadgesOnProfile()).append("\n")
                                .append("SubscriptionRedemptionFailed            : ").append(ZonaRosaStore.inAppPayments().getSubscriptionRedemptionFailed()).append("\n")
                                .append("ShouldCancelBeforeNextAttempt           : ").append(ZonaRosaStore.inAppPayments().getShouldCancelSubscriptionBeforeNextSubscribeAttempt()).append("\n");
    }
  }

  private @NonNull String getPaymentMethod(@NonNull InAppPaymentData inAppPaymentData) {
    return inAppPaymentData.paymentMethodType.toString();
  }

  private @NonNull String getRedemptionStage(@NonNull InAppPaymentData inAppPaymentData) {
    if (inAppPaymentData.redemption == null) {
      return "null";
    } else {
      return inAppPaymentData.redemption.stage.name();
    }
  }

  private @NonNull String getError(@NonNull InAppPaymentData inAppPaymentData) {
    if (inAppPaymentData.error == null) {
      return "none";
    } else {
      return inAppPaymentData.error.toString();
    }
  }

  private @NonNull String getCancellation(@NonNull InAppPaymentData inAppPaymentData) {
    if (inAppPaymentData.cancellation == null) {
      return "none";
    } else {
      return inAppPaymentData.cancellation.reason.name();
    }
  }
}
