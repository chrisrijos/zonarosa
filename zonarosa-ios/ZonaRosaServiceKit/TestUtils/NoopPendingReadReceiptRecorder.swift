//
// Copyright 2020 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

public class NoopPendingReceiptRecorder: PendingReceiptRecorder {
    public func recordPendingReadReceipt(for message: TSIncomingMessage, thread: TSThread, transaction: DBWriteTransaction) {
        Logger.info("")
    }

    public func recordPendingViewedReceipt(for message: TSIncomingMessage, thread: TSThread, transaction: DBWriteTransaction) {
        Logger.info("")
    }
}
