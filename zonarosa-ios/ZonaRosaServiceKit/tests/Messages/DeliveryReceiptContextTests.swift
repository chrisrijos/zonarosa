//
// Copyright 2022 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import XCTest
@testable import ZonaRosaServiceKit

class DeliveryReceiptContextTests: SSKBaseTest {
    func testExecutesDifferentMessages() throws {
        let aliceRecipient = ZonaRosaServiceAddress(phoneNumber: "+12345678900")
        let message = write { transaction in
            let aliceContactThread = TSContactThread.getOrCreateThread(withContactAddress: aliceRecipient, transaction: transaction)
            let helloAlice = TSOutgoingMessage(in: aliceContactThread, messageBody: "Hello Alice")
            helloAlice.anyInsert(transaction: transaction)
            return helloAlice
        }
        write { transaction in
            var messages = [TSOutgoingMessage]()
            BatchingDeliveryReceiptContext.withDeferredUpdates(transaction: transaction) { context in
                context.addUpdate(message: message, transaction: transaction) { m in
                    messages.append(m)
                }
            }
            XCTAssertEqual(messages.count, 2)
            XCTAssertFalse(messages[0] === messages[1])
        }

    }
}

// MARK: -

private extension TSOutgoingMessage {
    convenience init(in thread: TSThread, messageBody: String) {
        let builder: TSOutgoingMessageBuilder = .withDefaultValues(
            thread: thread,
            messageBody: AttachmentContentValidatorMock.mockValidatedBody(messageBody),
        )
        self.init(outgoingMessageWith: builder, recipientAddressStates: [:])
    }
}
