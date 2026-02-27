//
// Copyright 2022 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

extension Stripe {
    public struct StripeError: Error {
        public let code: String
    }
}
