//
// Copyright 2020 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation

public struct DarwinNotificationName: ExpressibleByStringLiteral {
    static let primaryDBFolderNameDidChange: DarwinNotificationName = "io.zonarosa.primaryDBFolderNameDidChange"

    static func sdsCrossProcess(for type: AppContextType) -> DarwinNotificationName {
        DarwinNotificationName("io.zonarosa.sdscrossprocess.\(type)")
    }

    static func connectionLock(for priority: Int) -> DarwinNotificationName {
        return DarwinNotificationName("io.zonarosa.connection.\(priority)")
    }

    public typealias StringLiteralType = String

    public let rawValue: String

    public init(stringLiteral name: String) {
        owsPrecondition(!name.isEmpty)
        self.rawValue = name
    }

    public init(_ name: String) {
        owsAssertDebug(!name.isEmpty)
        self.rawValue = name
    }
}
