//
// Copyright 2024 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import LibZonaRosaClient

struct GroupSendFullTokenBuilder {
    var secretParams: GroupSecretParams
    var expiration: Date
    var endorsement: GroupSendEndorsement

    func build() -> GroupSendFullToken {
        return endorsement.toFullToken(groupParams: secretParams, expiration: expiration)
    }
}
