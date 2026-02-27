//
// Copyright 2026 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import ZonaRosaFfi

public protocol UnauthProfilesService: Sendable {
    /// Does an account with the given ACI or PNI exist?
    ///
    /// Throws only if the request can't be completed.
    func accountExists(_ account: ServiceId) async throws -> Bool
}

extension UnauthenticatedChatConnection: UnauthProfilesService {
    public func accountExists(_ account: ServiceId) async throws -> Bool {
        return try await self.tokioAsyncContext
            .invokeAsyncFunction { promise, tokioAsyncContext in
                withNativeHandle { chatService in
                    account.withPointerToFixedWidthBinary { account in
                        zonarosa_unauthenticated_chat_connection_account_exists(
                            promise,
                            tokioAsyncContext.const(),
                            chatService.const(),
                            account
                        )
                    }
                }
            }
    }
}

extension UnauthServiceSelector where Self == UnauthServiceSelectorHelper<any UnauthProfilesService> {
    public static var profiles: Self { .init() }
}
