//
// Copyright 2024 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import LibZonaRosaClient
import ZonaRosaServiceKit
import ZonaRosaUI

enum CallTarget {
    case individual(TSContactThread)
    case groupThread(GroupIdentifier)
    case callLink(CallLink)
}

extension TSContactThread {
    var canCall: Bool {
        return !isNoteToSelf
    }
}

extension TSGroupThread {
    var canCall: Bool {
        return
            isGroupV2Thread
                && groupMembership.isLocalUserFullMember

    }
}
