//
// Copyright 2016 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation

// MARK: -

public class MessageFetcherJob {
    public init() {
        SwiftSingletons.register(self)
    }

    public var hasCompletedInitialFetch: Bool {
        get async {
            let chatConnectionManager = DependenciesBridge.shared.chatConnectionManager
            return await chatConnectionManager.hasEmptiedInitialQueue
        }
    }

    func preconditionForFetchingComplete() -> some Precondition {
        return NotificationPrecondition(
            notificationName: OWSChatConnection.chatConnectionStateDidChange,
            isSatisfied: { await self.hasCompletedInitialFetch },
        )
    }
}
