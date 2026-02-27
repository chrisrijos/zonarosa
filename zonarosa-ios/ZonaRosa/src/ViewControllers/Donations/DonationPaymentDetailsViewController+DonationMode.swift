//
// Copyright 2022 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import ZonaRosaServiceKit

extension DonationPaymentDetailsViewController {
    enum DonationMode {
        case oneTime
        case monthly(
            subscriptionLevel: DonationSubscriptionLevel,
            subscriberID: Data?,
            currentSubscription: Subscription?,
            currentSubscriptionLevel: DonationSubscriptionLevel?,
        )
        case gift(thread: TSContactThread, messageText: String)
    }
}
