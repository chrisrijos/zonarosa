//
// Copyright 2020-2021 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import ZonaRosaFfi

public class ProfileKeyCredentialRequestContext: ByteArray, @unchecked Sendable {
    public required init(contents: Data) throws {
        try super.init(contents, checkValid: zonarosa_profile_key_credential_request_context_check_valid_contents)
    }

    public func getRequest() throws -> ProfileKeyCredentialRequest {
        return try withUnsafePointerToSerialized { contents in
            try invokeFnReturningSerialized {
                zonarosa_profile_key_credential_request_context_get_request($0, contents)
            }
        }
    }
}
