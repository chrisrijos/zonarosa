//
// Copyright 2022 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import ZonaRosaFfi

public class ExpiringProfileKeyCredentialResponse: ByteArray, @unchecked Sendable {
    public required init(contents: Data) throws {
        try super.init(contents, checkValid: zonarosa_expiring_profile_key_credential_response_check_valid_contents)
    }
}
