/*
 * Copyright 2022 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.subscriptions;

public enum PaymentMethod {
  UNKNOWN,
  /**
   * A credit card or debit card, including those from Apple Pay and Google Pay
   */
  CARD,
  /**
   * A PayPal account
   */
  PAYPAL,
  /**
   * A SEPA debit account
   */
  SEPA_DEBIT,
  /**
   * An iDEAL account
   */
  IDEAL,
  GOOGLE_PLAY_BILLING,
  APPLE_APP_STORE
}
