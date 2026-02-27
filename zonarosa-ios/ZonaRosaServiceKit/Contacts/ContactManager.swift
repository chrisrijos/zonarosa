//
// Copyright 2024 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

public import Contacts
import Foundation

public protocol ContactManager: ContactsManagerProtocol {
    func fetchZonaRosaAccounts(for phoneNumbers: [String], transaction: DBReadTransaction) -> [ZonaRosaAccount?]

    func displayNames(for addresses: [ZonaRosaServiceAddress], tx: DBReadTransaction) -> [DisplayName]

    func cnContactId(for phoneNumber: String) -> String?

    func cnContact(withId cnContactId: String?) -> CNContact?
}

extension ContactManager {
    public func fetchZonaRosaAccount(for address: ZonaRosaServiceAddress, transaction: DBReadTransaction) -> ZonaRosaAccount? {
        guard let phoneNumber = address.phoneNumber else {
            return nil
        }
        return fetchZonaRosaAccount(forPhoneNumber: phoneNumber, transaction: transaction)
    }

    public func fetchZonaRosaAccount(forPhoneNumber phoneNumber: String, transaction: DBReadTransaction) -> ZonaRosaAccount? {
        return fetchZonaRosaAccounts(for: [phoneNumber], transaction: transaction)[0]
    }

    public func systemContactName(for phoneNumber: String, tx transaction: DBReadTransaction) -> DisplayName.SystemContactName? {
        return systemContactNames(for: [phoneNumber], tx: transaction)[0]
    }

    public func systemContactNames(for phoneNumbers: [String], tx: DBReadTransaction) -> [DisplayName.SystemContactName?] {
        return fetchZonaRosaAccounts(for: phoneNumbers, transaction: tx).map {
            guard let nameComponents = $0?.contactNameComponents() else {
                return nil
            }
            return DisplayName.SystemContactName(
                nameComponents: nameComponents,
                multipleAccountLabel: $0?.multipleAccountLabelText,
            )
        }
    }

    public func displayName(for address: ZonaRosaServiceAddress, tx: DBReadTransaction) -> DisplayName {
        return displayNames(for: [address], tx: tx)[0]
    }

    public func avatarData(for cnContactId: String?) -> Data? {
        // Don't bother to cache avatar data.
        guard let cnContact = self.cnContact(withId: cnContactId) else {
            return nil
        }
        return avatarData(for: cnContact)
    }

    public func avatarData(for cnContact: CNContact) -> Data? {
        return SystemContact.avatarData(for: cnContact)
    }

    public func avatarImage(for cnContactId: String?) -> UIImage? {
        guard let avatarData = self.avatarData(for: cnContactId) else {
            return nil
        }
        guard DataImageSource(avatarData).ows_isValidImage else {
            Logger.warn("Invalid image.")
            return nil
        }
        guard let avatarImage = UIImage(data: avatarData) else {
            Logger.warn("Couldn't load image.")
            return nil
        }
        return avatarImage
    }
}
