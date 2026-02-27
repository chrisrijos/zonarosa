//
// Copyright 2023 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation

public class DonationSubscriptionLevel: Comparable, Equatable, Codable {
    public let level: UInt
    public let badge: ProfileBadge
    public let amounts: [Currency.Code: FiatMoney]

    public init(
        level: UInt,
        badge: ProfileBadge,
        amounts: [Currency.Code: FiatMoney],
    ) {
        self.level = level
        self.badge = badge
        self.amounts = amounts
    }

    // MARK: Comparable

    public static func <(lhs: DonationSubscriptionLevel, rhs: DonationSubscriptionLevel) -> Bool {
        return lhs.level < rhs.level
    }

    public static func ==(lhs: DonationSubscriptionLevel, rhs: DonationSubscriptionLevel) -> Bool {
        return lhs.level == rhs.level
    }
}
