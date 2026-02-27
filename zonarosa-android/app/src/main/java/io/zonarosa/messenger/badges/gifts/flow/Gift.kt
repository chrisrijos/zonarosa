package io.zonarosa.messenger.badges.gifts.flow

import io.zonarosa.core.util.money.FiatMoney

/**
 * Convenience wrapper for a gift at a particular price point.
 */
data class Gift(val level: Long, val price: FiatMoney)
