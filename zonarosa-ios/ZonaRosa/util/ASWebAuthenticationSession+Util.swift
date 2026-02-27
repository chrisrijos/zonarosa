//
// Copyright 2022 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import AuthenticationServices
import ZonaRosaServiceKit

extension ASWebAuthenticationSession {
    /// Convert an ``ASWebAuthenticationSession`` completion's arguments into a single ``Result``.
    ///
    /// ``ASWebAuthenticationSession.CompletionHandler`` is called with two arguments: a result
    /// URL or an error. Exactly one should be defined. This function helps ensure that with types.
    class func resultify(callbackUrl: URL?, error: Error?) -> Result<URL, Error> {
        if let error {
            owsAssertDebug(
                callbackUrl == nil,
                "ASWebAuthenticationSession returned an error and a callback URL. Does iOS have a bug?",
            )
            return .failure(error)
        }

        if let callbackUrl {
            return .success(callbackUrl)
        }

        owsFail("ASWebAuthenticationSession returned neither a callback URL nor an error. Does iOS have a bug?")
    }
}
