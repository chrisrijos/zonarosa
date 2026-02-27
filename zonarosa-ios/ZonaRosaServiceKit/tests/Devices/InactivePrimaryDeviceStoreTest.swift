//
// Copyright 2025 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import XCTest
@testable import ZonaRosaServiceKit

class InactivePrimaryDeviceStoreTest: XCTestCase {
    private let db: any DB = InMemoryDB()
    private var inactivePrimaryDeviceStore: InactivePrimaryDeviceStore!

    override func setUp() {
        inactivePrimaryDeviceStore = InactivePrimaryDeviceStore()
    }

    func testSetIdlePrimaryDevice() {
        db.read { tx in
            XCTAssertFalse(inactivePrimaryDeviceStore.valueForInactivePrimaryDeviceAlert(transaction: tx))
        }

        db.write { tx in
            inactivePrimaryDeviceStore.setValueForInactivePrimaryDeviceAlert(value: true, transaction: tx)
        }

        db.read { tx in
            XCTAssertTrue(inactivePrimaryDeviceStore.valueForInactivePrimaryDeviceAlert(transaction: tx))
        }

        db.write { tx in
            inactivePrimaryDeviceStore.setValueForInactivePrimaryDeviceAlert(value: false, transaction: tx)
        }

        db.read { tx in
            XCTAssertFalse(inactivePrimaryDeviceStore.valueForInactivePrimaryDeviceAlert(transaction: tx))
        }
    }
}
