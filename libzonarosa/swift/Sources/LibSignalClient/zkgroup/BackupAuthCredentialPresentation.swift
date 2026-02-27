//
// Copyright 2023 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import ZonaRosaFfi

public class BackupAuthCredentialPresentation: ByteArray, @unchecked Sendable {
    public required init(contents: Data) throws {
        try super.init(contents, checkValid: zonarosa_backup_auth_credential_presentation_check_valid_contents)
    }

    public func verify(now: Date = Date(), serverParams: GenericServerSecretParams) throws {
        try withAllBorrowed(self, serverParams) { contents, serverParams in
            try checkError(
                zonarosa_backup_auth_credential_presentation_verify(
                    contents,
                    UInt64(now.timeIntervalSince1970),
                    serverParams
                )
            )
        }
    }
}
