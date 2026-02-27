//
// Copyright 2023 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import ZonaRosaServiceKit

class AppIconBadgeUpdater {
    private let badgeManager: BadgeManager

    init(badgeManager: BadgeManager) {
        self.badgeManager = badgeManager
    }

    func startObserving() {
        badgeManager.addObserver(self)
    }
}

extension AppIconBadgeUpdater: BadgeObserver {
    func didUpdateBadgeCount(_ badgeManager: BadgeManager, badgeCount: BadgeCount) {
        UIApplication.shared.applicationIconBadgeNumber = Int(badgeCount.unreadTotalCount)
    }
}
