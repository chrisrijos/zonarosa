//
// Copyright 2023 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

/// Service providers who process donation payments on our behalf.
public enum DonationPaymentProcessor: String {
    /// Represents Stripe, which we use for Apple Pay, credit/debit card, and
    /// SEPA debit payments.
    case stripe = "STRIPE"

    /// Represents Braintree, which we use for PayPal payments.
    case braintree = "BRAINTREE"
}
