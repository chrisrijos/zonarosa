//
// Copyright 2022 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import ZonaRosaFfi

///
/// Cds2Client provides bindings to interact with ZonaRosa's v2 Contact Discovery Service.
///
/// See ``SgxClient``
public class Cds2Client: SgxClient {
    public convenience init(
        mrenclave: some ContiguousBytes,
        attestationMessage: some ContiguousBytes,
        currentDate: Date
    ) throws {
        let handle = try attestationMessage.withUnsafeBorrowedBuffer { attestationMessageBuffer in
            try mrenclave.withUnsafeBorrowedBuffer { mrenclaveBuffer in
                try invokeFnReturningValueByPointer(.init()) {
                    zonarosa_cds2_client_state_new(
                        $0,
                        mrenclaveBuffer,
                        attestationMessageBuffer,
                        UInt64(currentDate.timeIntervalSince1970 * 1000)
                    )
                }
            }
        }
        self.init(owned: NonNull(handle)!)
    }
}
