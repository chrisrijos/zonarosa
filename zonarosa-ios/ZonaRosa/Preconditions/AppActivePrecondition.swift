//
// Copyright 2023 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import ZonaRosaServiceKit

struct AppActivePrecondition: Precondition {
    private let _precondition: NotificationPrecondition

    init(appContext: AppContext) {
        self._precondition = NotificationPrecondition(
            notificationName: UIApplication.didBecomeActiveNotification,
            isSatisfied: { appContext.isAppForegroundAndActive() },
        )
    }

    func waitUntilSatisfied() async -> WaitResult {
        return await self._precondition.waitUntilSatisfied()
    }
}
