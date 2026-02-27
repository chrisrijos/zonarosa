package io.zonarosa.messenger.components.settings.app.internal.donor

import io.zonarosa.donations.StripeDeclineCode
import io.zonarosa.messenger.badges.models.Badge
import io.zonarosa.messenger.components.settings.app.subscription.errors.UnexpectedSubscriptionCancellation

data class InternalDonorErrorConfigurationState(
  val badges: List<Badge> = emptyList(),
  val selectedBadge: Badge? = null,
  val selectedUnexpectedSubscriptionCancellation: UnexpectedSubscriptionCancellation? = null,
  val selectedStripeDeclineCode: StripeDeclineCode.Code? = null
)
