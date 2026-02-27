//
// Copyright 2023 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import ZonaRosaFfi

///
/// Svr2Client provides bindings to interact with ZonaRosa's v2 Secure Value Recovery service.
///
/// See ``SgxClient``
public class Svr2Client: SgxClient {
    public convenience init(
        mrenclave: some ContiguousBytes,
        attestationMessage: some ContiguousBytes,
        currentDate: Date
    ) throws {
        let handle = try attestationMessage.withUnsafeBorrowedBuffer { attestationMessageBuffer in
            try mrenclave.withUnsafeBorrowedBuffer { mrenclaveBuffer in
                try invokeFnReturningValueByPointer(.init()) {
                    zonarosa_svr2_client_new(
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
