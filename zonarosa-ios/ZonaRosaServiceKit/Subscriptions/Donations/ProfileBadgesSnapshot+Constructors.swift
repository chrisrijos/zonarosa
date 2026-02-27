//
// Copyright 2023 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation

public extension ProfileBadgesSnapshot {
    static func forLocalProfile(profileManager: any ProfileManager, tx: DBReadTransaction) -> ProfileBadgesSnapshot {
        let badgeInfos = profileManager.localUserProfile(tx: tx)?.badges ?? []
        return ProfileBadgesSnapshot(existingBadges: badgeInfos.map {
            return ProfileBadgesSnapshot.Badge(id: $0.badgeId, isVisible: $0.isVisible ?? false)
        })
    }
}
