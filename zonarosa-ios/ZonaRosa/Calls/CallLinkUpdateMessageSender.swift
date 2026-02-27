//
// Copyright 2024 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import ZonaRosaRingRTC
import ZonaRosaServiceKit

final class CallLinkUpdateMessageSender {
    private let messageSenderJobQueue: MessageSenderJobQueue

    init(messageSenderJobQueue: MessageSenderJobQueue) {
        self.messageSenderJobQueue = messageSenderJobQueue
    }

    func sendCallLinkUpdateMessage(rootKey: CallLinkRootKey, adminPasskey: Data?, tx: DBWriteTransaction) {
        let localThread = TSContactThread.getOrCreateLocalThread(transaction: tx)!
        let callLinkUpdate = OutgoingCallLinkUpdateMessage(
            localThread: localThread,
            rootKey: rootKey,
            adminPasskey: adminPasskey,
            tx: tx,
        )
        messageSenderJobQueue.add(message: .preprepared(transientMessageWithoutAttachments: callLinkUpdate), transaction: tx)
    }
}
