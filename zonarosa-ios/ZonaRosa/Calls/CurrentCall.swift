//
// Copyright 2024 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import LibZonaRosaClient
import ZonaRosaServiceKit

struct CurrentCall {
    private let rawValue: AtomicValue<ZonaRosaCall?>

    init(rawValue: AtomicValue<ZonaRosaCall?>) {
        self.rawValue = rawValue
    }

    func get() -> ZonaRosaCall? { rawValue.get() }
}

extension CurrentCall: CurrentCallProvider {
    var hasCurrentCall: Bool { self.get() != nil }
    var currentGroupThreadCallGroupId: GroupIdentifier? {
        switch self.get()?.mode {
        case nil, .individual, .callLink:
            return nil
        case .groupThread(let call):
            return call.groupId
        }
    }
}
