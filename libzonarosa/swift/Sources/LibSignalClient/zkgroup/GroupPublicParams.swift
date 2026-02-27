//
// Copyright 2020-2021 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import ZonaRosaFfi

public class GroupPublicParams: ByteArray, @unchecked Sendable {
    public required init(contents: Data) throws {
        try super.init(contents, checkValid: zonarosa_group_public_params_check_valid_contents)
    }

    public func getGroupIdentifier() throws -> GroupIdentifier {
        return try withUnsafePointerToSerialized { contents in
            try invokeFnReturningSerialized {
                zonarosa_group_public_params_get_group_identifier($0, contents)
            }
        }
    }
}
