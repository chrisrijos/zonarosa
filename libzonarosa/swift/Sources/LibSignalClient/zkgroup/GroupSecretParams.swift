//
// Copyright 2020-2021 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import ZonaRosaFfi

public class GroupSecretParams: ByteArray, @unchecked Sendable {
    public static func generate() throws -> GroupSecretParams {
        return try self.generate(randomness: Randomness.generate())
    }

    public static func generate(randomness: Randomness) throws -> GroupSecretParams {
        return try randomness.withUnsafePointerToBytes { randomness in
            try invokeFnReturningSerialized {
                zonarosa_group_secret_params_generate_deterministic($0, randomness)
            }
        }
    }

    public static func deriveFromMasterKey(groupMasterKey: GroupMasterKey) throws -> GroupSecretParams {
        return try groupMasterKey.withUnsafePointerToSerialized { groupMasterKey in
            try invokeFnReturningSerialized {
                zonarosa_group_secret_params_derive_from_master_key($0, groupMasterKey)
            }
        }
    }

    public required init(contents: Data) throws {
        try super.init(contents, checkValid: zonarosa_group_secret_params_check_valid_contents)
    }

    public func getMasterKey() throws -> GroupMasterKey {
        return try withUnsafePointerToSerialized { contents in
            try invokeFnReturningSerialized {
                zonarosa_group_secret_params_get_master_key($0, contents)
            }
        }
    }

    public func getPublicParams() throws -> GroupPublicParams {
        return try withUnsafePointerToSerialized { contents in
            try invokeFnReturningSerialized {
                zonarosa_group_secret_params_get_public_params($0, contents)
            }
        }
    }
}
