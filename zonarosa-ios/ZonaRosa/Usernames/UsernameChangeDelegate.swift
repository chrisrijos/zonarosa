//
// Copyright 2023 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import ZonaRosaServiceKit

/// Represents an observer who should be notified immediately when username
/// state may have changed.
protocol UsernameChangeDelegate: AnyObject {
    func usernameStateDidChange(newState: Usernames.LocalUsernameState)
}
