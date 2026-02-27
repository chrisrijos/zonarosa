//
// Copyright 2024 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import ZonaRosaFfi

public enum BackupLevel: UInt8, Sendable {
    // This must match the Rust version of the enum.
    case free = 200
    case paid = 201
}

public enum BackupCredentialType: UInt8 {
    // This must match the Rust version of the enum.
    case messages = 1
    case media = 2
}
