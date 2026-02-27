//
// Copyright 2023 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

/// A logger for username-related events.
public class UsernameLogger: PrefixedLogger {
    public static let shared: UsernameLogger = .init()

    private init() {
        super.init(prefix: "[Username]")
    }
}
