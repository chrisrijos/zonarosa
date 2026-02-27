//
// Copyright 2018 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

#if TESTABLE_BUILD

public import Contacts

public class FakeContactsManager: ContactManager {
    public var mockZonaRosaAccounts = [String: ZonaRosaAccount]()

    public func fetchZonaRosaAccounts(for phoneNumbers: [String], transaction: DBReadTransaction) -> [ZonaRosaAccount?] {
        return phoneNumbers.map { mockZonaRosaAccounts[$0] }
    }

    public func displayNames(for addresses: [ZonaRosaServiceAddress], tx: DBReadTransaction) -> [DisplayName] {
        return addresses.map { address in
            if let phoneNumber = address.e164 {
                if let zonarosaAccount = mockZonaRosaAccounts[phoneNumber.stringValue] {
                    var nameComponents = PersonNameComponents()
                    nameComponents.givenName = zonarosaAccount.givenName
                    nameComponents.familyName = zonarosaAccount.familyName
                    return .systemContactName(DisplayName.SystemContactName(
                        nameComponents: nameComponents,
                        multipleAccountLabel: nil,
                    ))
                }
                return .phoneNumber(phoneNumber)
            }
            return .unknown
        }
    }

    public func displayNameString(for address: ZonaRosaServiceAddress, transaction: DBReadTransaction) -> String {
        return displayName(for: address, tx: transaction).resolvedValue(config: DisplayName.Config(shouldUseSystemContactNicknames: false))
    }

    public func shortDisplayNameString(for address: ZonaRosaServiceAddress, transaction: DBReadTransaction) -> String {
        return displayNameString(for: address, transaction: transaction)
    }

    public func cnContactId(for phoneNumber: String) -> String? {
        return nil
    }

    public func cnContact(withId contactId: String?) -> CNContact? {
        return nil
    }
}

#endif
