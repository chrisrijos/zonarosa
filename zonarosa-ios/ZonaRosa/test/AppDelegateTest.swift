//
// Copyright 2022 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import XCTest
@testable import ZonaRosa

class AppDelegateTest: XCTestCase {
    func testApplicationShortcutItems() throws {
        func hasNewMessageShortcut(_ shortcuts: [UIApplicationShortcutItem]) -> Bool {
            shortcuts.contains(where: { $0.type.contains("quickCompose") })
        }

        let unregistered = AppDelegate.applicationShortcutItems(isRegistered: false)
        XCTAssertFalse(hasNewMessageShortcut(unregistered))

        let registered = AppDelegate.applicationShortcutItems(isRegistered: true)
        XCTAssertTrue(hasNewMessageShortcut(registered))
    }
}
