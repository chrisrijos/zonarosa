//
// Copyright 2023 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation

public enum PreparedGiftPayment {
    case forStripe(paymentIntent: Stripe.PaymentIntent, paymentMethodId: String)
    case forPaypal(approvalParams: Paypal.OneTimePaymentWebAuthApprovalParams, paymentId: String)
}
