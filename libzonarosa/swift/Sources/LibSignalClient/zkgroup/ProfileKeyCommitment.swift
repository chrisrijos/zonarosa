//
// Copyright 2020-2021 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import ZonaRosaFfi

public class ProfileKeyCommitment: ByteArray, @unchecked Sendable {
    public required init(contents: Data) throws {
        try super.init(contents, checkValid: zonarosa_profile_key_commitment_check_valid_contents)
    }
}
