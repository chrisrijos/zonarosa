//
// Copyright 2023 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation

#if TESTABLE_BUILD

open class SentMessageTranscriptReceiverMock: SentMessageTranscriptReceiver {

    public init() {}

    public func process(
        _: SentMessageTranscript,
        localIdentifiers: LocalIdentifiers,
        tx: DBWriteTransaction,
    ) -> Result<TSOutgoingMessage?, Error> {
        // Do nothing
        return .success(nil)
    }
}

#endif
