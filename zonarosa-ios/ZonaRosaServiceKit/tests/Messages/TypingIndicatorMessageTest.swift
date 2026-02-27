//
// Copyright 2022 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import ZonaRosaServiceKit
import XCTest

class TypingIndicatorMessageTest: SSKBaseTest {
    private func makeThread(transaction: DBWriteTransaction) -> TSThread {
        TSContactThread.getOrCreateThread(
            withContactAddress: ZonaRosaServiceAddress(phoneNumber: "+12223334444"),
            transaction: transaction,
        )
    }

    func testIsOnline() throws {
        write { transaction in
            let message = TypingIndicatorMessage(
                thread: makeThread(transaction: transaction),
                action: .started,
                transaction: transaction,
            )
            XCTAssertTrue(message.isOnline)
        }
    }

    func testIsUrgent() throws {
        write { transaction in
            let message = TypingIndicatorMessage(
                thread: makeThread(transaction: transaction),
                action: .started,
                transaction: transaction,
            )
            XCTAssertFalse(message.isUrgent)
        }
    }
}
