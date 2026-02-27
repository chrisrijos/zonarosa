//
// Copyright 2023 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import ZonaRosaFfi

public class CallLinkAuthCredential: ByteArray, @unchecked Sendable {
    public required init(contents: Data) throws {
        try super.init(contents, checkValid: zonarosa_call_link_auth_credential_check_valid_contents)
    }

    public func present(
        userId: Aci,
        redemptionTime: Date,
        serverParams: GenericServerPublicParams,
        callLinkParams: CallLinkSecretParams
    ) -> CallLinkAuthCredentialPresentation {
        return failOnError {
            self.present(
                userId: userId,
                redemptionTime: redemptionTime,
                serverParams: serverParams,
                callLinkParams: callLinkParams,
                randomness: try .generate()
            )
        }
    }

    public func present(
        userId: Aci,
        redemptionTime: Date,
        serverParams: GenericServerPublicParams,
        callLinkParams: CallLinkSecretParams,
        randomness: Randomness
    ) -> CallLinkAuthCredentialPresentation {
        return failOnError {
            try withAllBorrowed(
                self,
                userId,
                serverParams,
                callLinkParams,
                randomness
            ) { contents, userId, serverParams, callLinkParams, randomness in
                try invokeFnReturningVariableLengthSerialized {
                    zonarosa_call_link_auth_credential_present_deterministic(
                        $0,
                        contents,
                        userId,
                        UInt64(redemptionTime.timeIntervalSince1970),
                        serverParams,
                        callLinkParams,
                        randomness
                    )
                }
            }
        }
    }
}
