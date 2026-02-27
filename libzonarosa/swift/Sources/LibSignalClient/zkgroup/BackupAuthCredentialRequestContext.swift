//
// Copyright 2023 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import ZonaRosaFfi

public class BackupAuthCredentialRequestContext: ByteArray, @unchecked Sendable {
    public required init(contents: Data) throws {
        try super.init(contents, checkValid: zonarosa_backup_auth_credential_request_context_check_valid_contents)
    }

    public static func create<BackupKey: ContiguousBytes>(backupKey: BackupKey, aci: UUID) -> Self {
        return failOnError {
            let backupKeyBytes = try backupKey.withUnsafeBytes {
                try ByteArray(newContents: Data($0), expectedLength: 32)
            }
            return try withAllBorrowed(.fixed(backupKeyBytes), aci) { backupKeyTuple, uuid in
                try invokeFnReturningVariableLengthSerialized {
                    zonarosa_backup_auth_credential_request_context_new($0, backupKeyTuple, uuid)
                }
            }
        }
    }

    public func getRequest() -> BackupAuthCredentialRequest {
        return failOnError {
            try withUnsafeBorrowedBuffer { contents in
                try invokeFnReturningVariableLengthSerialized {
                    zonarosa_backup_auth_credential_request_context_get_request($0, contents)
                }
            }
        }
    }

    public func receive(
        _ response: BackupAuthCredentialResponse,
        timestamp: Date,
        params: GenericServerPublicParams
    ) throws -> BackupAuthCredential {
        return try withAllBorrowed(self, response, params) { contents, response, params in
            try invokeFnReturningVariableLengthSerialized {
                zonarosa_backup_auth_credential_request_context_receive_response(
                    $0,
                    contents,
                    response,
                    UInt64(timestamp.timeIntervalSince1970),
                    params
                )
            }
        }
    }
}
