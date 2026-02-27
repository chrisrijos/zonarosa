//
// Copyright 2022 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import LibZonaRosaClient
import XCTest

@testable import ZonaRosaServiceKit

class ZonaRosaAccountFinderTest: SSKBaseTest {
    override func setUp() {
        super.setUp()
        // Create local account.
        SSKEnvironment.shared.databaseStorageRef.write { tx in
            (DependenciesBridge.shared.registrationStateChangeManager as! RegistrationStateChangeManagerImpl).registerForTests(
                localIdentifiers: .forUnitTests,
                tx: tx,
            )
        }
    }

    private func createAccount(phoneNumber: E164) -> ZonaRosaAccount {
        write {
            let account = ZonaRosaAccount(phoneNumber: phoneNumber.stringValue)
            account.anyInsert(transaction: $0)
            return account
        }
    }

    func testFetchAccounts() {
        let pn1 = E164("+16505550100")!
        let account1 = createAccount(phoneNumber: pn1)

        // Nothing prevents us from creating multiple accounts for the same recipient.
        let pn3 = E164("+16505550101")!
        let account3 = createAccount(phoneNumber: pn3)
        _ = createAccount(phoneNumber: pn3)

        // Create an account but don't fetch it.
        let pn4 = E164("+16505550102")!
        _ = createAccount(phoneNumber: pn4)

        // Create a phone number without an account.
        let pn5 = E164("+16505550103")!

        let phoneNumbersToFetch: [E164] = [
            pn1,
            pn3,
            pn5,
        ]

        let expectedAccounts: [ZonaRosaAccount?] = [
            account1,
            account3,
            nil,
        ]

        read { tx in
            let accountFinder = ZonaRosaAccountFinder()
            let actualAccounts = accountFinder.zonarosaAccounts(for: phoneNumbersToFetch.map(\.stringValue), tx: tx)
            XCTAssertEqual(
                actualAccounts.map { $0?.recipientPhoneNumber },
                expectedAccounts.map { $0?.recipientPhoneNumber },
            )
        }
    }
}
