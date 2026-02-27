//
// Copyright 2022 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import ZonaRosaServiceKit

struct ProxyConnectionChecker {
    private let chatConnectionManager: any ChatConnectionManager

    init(chatConnectionManager: any ChatConnectionManager) {
        self.chatConnectionManager = chatConnectionManager
    }

    func checkConnection() async -> Bool {
        do {
            try await withCooperativeTimeout(seconds: OWSRequestFactory.textSecureHTTPTimeOut) {
                try await chatConnectionManager.waitForUnidentifiedConnectionToOpen()
            }
            return true
        } catch {
            return false
        }
    }
}
