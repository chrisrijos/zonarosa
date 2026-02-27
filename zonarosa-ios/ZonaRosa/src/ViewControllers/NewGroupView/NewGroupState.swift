//
// Copyright 2020 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import ZonaRosaServiceKit
import ZonaRosaUI

class NewGroupState {

    var groupSeed = NewGroupSeed()

    var recipientSet = OrderedSet<PickedRecipient>()

    var groupName: String?

    var avatarData: Data?

    func deriveNewGroupSeedForRetry() {
        groupSeed = NewGroupSeed()
    }

    var hasUnsavedChanges: Bool {
        return !recipientSet.isEmpty && groupName == nil && avatarData == nil
    }
}
