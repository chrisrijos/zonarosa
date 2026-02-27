//
// Copyright 2023 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import LibZonaRosaClient
import XCTest

@testable import ZonaRosaServiceKit

final class RecipientStateMergerTest: XCTestCase {
    private var mockDB: InMemoryDB!
    private var _zonarosaServiceAddressCache: ZonaRosaServiceAddressCache!
    private var recipientDatabaseTable: RecipientDatabaseTable!
    private var recipientStateMerger: RecipientStateMerger!

    override func setUp() {
        super.setUp()

        mockDB = InMemoryDB()
        _zonarosaServiceAddressCache = ZonaRosaServiceAddressCache()
        recipientDatabaseTable = RecipientDatabaseTable()
        recipientStateMerger = RecipientStateMerger(
            recipientDatabaseTable: recipientDatabaseTable,
            zonarosaServiceAddressCache: _zonarosaServiceAddressCache,
        )
    }

    func testNormalize() {
        let aci1 = Aci.constantForTesting("00000000-0000-4000-8000-0000000000a1")
        let pni1 = Pni.constantForTesting("PNI:00000000-0000-4000-8000-0000000000b1")
        let aci2 = Aci.constantForTesting("00000000-0000-4000-8000-0000000000a2")
        let pni3 = Pni.constantForTesting("PNI:00000000-0000-4000-8000-0000000000b3")
        let aci4 = Aci.constantForTesting("00000000-0000-4000-8000-0000000000a4")
        let pni4 = Pni.constantForTesting("PNI:00000000-0000-4000-8000-0000000000b4")

        mockDB.write { tx in
            _ = try! ZonaRosaRecipient.insertRecord(aci: aci1, pni: pni1, tx: tx)
            _ = try! ZonaRosaRecipient.insertRecord(aci: aci4, pni: pni4, tx: tx)
        }

        var recipientStates: [ZonaRosaServiceAddress: TSOutgoingMessageRecipientState]? = [
            makeAddress(pni1): makeState(deliveryTimestamp: 1),
            makeAddress(aci2): makeState(deliveryTimestamp: 2),
            makeAddress(pni3): makeState(deliveryTimestamp: 3),
            makeAddress(aci4): makeState(deliveryTimestamp: 4),
            makeAddress(pni4): makeState(deliveryTimestamp: 5),
        ]
        mockDB.read { tx in
            recipientStateMerger.normalize(&recipientStates, tx: tx)
        }

        func assertDeliveryTimestamp(_ serviceId: ServiceId, equalTo: UInt64) {
            let recipientState = recipientStates!.removeValue(forKey: makeAddress(serviceId))
            XCTAssertEqual(recipientState?.status, .delivered)
            XCTAssertEqual(recipientState?.statusTimestamp, equalTo)
        }

        assertDeliveryTimestamp(aci1, equalTo: 1)
        assertDeliveryTimestamp(aci2, equalTo: 2)
        assertDeliveryTimestamp(pni3, equalTo: 3)
        assertDeliveryTimestamp(aci4, equalTo: 4)
        XCTAssertEqual(recipientStates, [:])
    }

    private func makeAddress(_ serviceId: ServiceId) -> ZonaRosaServiceAddress {
        return ZonaRosaServiceAddress(
            serviceId: serviceId,
            phoneNumber: nil,
            cache: _zonarosaServiceAddressCache,
        )
    }

    private func makeState(deliveryTimestamp: UInt64) -> TSOutgoingMessageRecipientState {
        return TSOutgoingMessageRecipientState(
            status: .delivered,
            statusTimestamp: deliveryTimestamp,
            wasSentByUD: false,
            errorCode: nil,
        )
    }
}
