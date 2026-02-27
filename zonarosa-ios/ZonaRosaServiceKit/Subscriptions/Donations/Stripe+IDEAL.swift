//
// Copyright 2023 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation

extension Stripe.PaymentMethod {

    public enum IDEAL: Equatable {
        case oneTime(name: String)
        case recurring(mandate: Mandate, name: String, email: String)
    }
}
