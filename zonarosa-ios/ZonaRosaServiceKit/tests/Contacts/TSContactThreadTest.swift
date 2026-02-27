//
// Copyright 2022 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import XCTest

@testable import ZonaRosaServiceKit

class TSContactThreadTest: SSKBaseTest {
    private func contactThread() -> TSContactThread {
        TSContactThread.getOrCreateThread(contactAddress: ZonaRosaServiceAddress.randomForTesting())
    }

    override func setUp() {
        super.setUp()
        SSKEnvironment.shared.databaseStorageRef.write { tx in
            (DependenciesBridge.shared.registrationStateChangeManager as! RegistrationStateChangeManagerImpl).registerForTests(
                localIdentifiers: .forUnitTests,
                tx: tx,
            )
        }
    }

    func testHasSafetyNumbersWithoutRemoteIdentity() {
        XCTAssertFalse(contactThread().hasSafetyNumbers())
    }

    func testHasSafetyNumbersWithRemoteIdentity() {
        let contactThread = self.contactThread()

        let identityManager = DependenciesBridge.shared.identityManager
        SSKEnvironment.shared.databaseStorageRef.write { tx in
            _ = identityManager.saveIdentityKey(Data(count: 32), for: contactThread.contactAddress.serviceId!, tx: tx)
        }

        XCTAssert(contactThread.hasSafetyNumbers())
    }

    func testCanSendChatMessagesToThread() {
        XCTAssertTrue(contactThread().canSendChatMessagesToThread())
    }
}
