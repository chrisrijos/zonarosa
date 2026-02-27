//
// Copyright 2024 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import LibZonaRosaClient

struct GroupSendEndorsements {
    var secretParams: GroupSecretParams
    var expiration: Date
    var combined: GroupSendEndorsement
    var individual: [ServiceId: GroupSendEndorsement]

    func tokenBuilder(forServiceId serviceId: ServiceId) -> GroupSendFullTokenBuilder? {
        return individual[serviceId].map {
            return GroupSendFullTokenBuilder(secretParams: secretParams, expiration: expiration, endorsement: $0)
        }
    }

    static func willExpireSoon(expirationDate: Date?) -> Bool {
        return expirationDate == nil || expirationDate!.timeIntervalSinceNow < 2 * .hour
    }
}
