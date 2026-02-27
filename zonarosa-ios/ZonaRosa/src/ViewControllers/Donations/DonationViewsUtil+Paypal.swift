//
// Copyright 2023 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import ZonaRosaServiceKit
import ZonaRosaUI

extension DonationViewsUtil {
    enum Paypal {
        /// Create a PayPal payment, returning a PayPal URL to present to the user
        /// for authentication. Presents an activity indicator while in-progress.
        @MainActor
        static func createPaypalPaymentBehindActivityIndicator(
            amount: FiatMoney,
            level: OneTimeBadgeLevel,
            fromViewController: UIViewController,
        ) async throws -> (URL, String) {
            return try await ModalActivityIndicatorViewController.presentAndPropagateResult(
                from: fromViewController,
            ) {
                return try await ZonaRosaServiceKit.Paypal.createBoost(
                    amount: amount,
                    level: level,
                )
            }
        }
    }
}
