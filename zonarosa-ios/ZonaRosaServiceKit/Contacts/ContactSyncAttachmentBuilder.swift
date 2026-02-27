//
// Copyright 2023 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Contacts
import Foundation
import LibZonaRosaClient

enum ContactSyncAttachmentBuilder {
    static func buildAttachmentFile(
        contactsManager: OWSContactsManager,
        tx: DBReadTransaction,
    ) -> URL? {
        let tsAccountManager = DependenciesBridge.shared.tsAccountManager
        guard let localAddress = tsAccountManager.localIdentifiers(tx: tx)?.aciAddress else {
            owsFailDebug("Missing localAddress.")
            return nil
        }

        let fileUrl = OWSFileSystem.temporaryFileUrl(
            fileExtension: nil,
            isAvailableWhileDeviceLocked: true,
        )
        guard let outputStream = OutputStream(url: fileUrl, append: false) else {
            owsFailDebug("Could not open outputStream.")
            return nil
        }
        let outputStreamDelegate = OWSStreamDelegate()
        outputStream.delegate = outputStreamDelegate
        outputStream.schedule(in: .current, forMode: .default)
        outputStream.open()
        guard outputStream.streamStatus == .open else {
            owsFailDebug("Could not open outputStream.")
            return nil
        }

        do {
            defer {
                outputStream.remove(from: .current, forMode: .default)
                outputStream.close()
            }
            try fetchAndWriteContacts(
                to: ContactOutputStream(outputStream: outputStream),
                localAddress: localAddress,
                contactsManager: contactsManager,
                tx: tx,
            )
        } catch {
            owsFailDebug("Could not write contacts sync stream: \(error)")
            return nil
        }

        guard outputStream.streamStatus == .closed, !outputStreamDelegate.hadError else {
            owsFailDebug("Could not close stream.")
            return nil
        }

        return fileUrl
    }

    private static func fetchAndWriteContacts(
        to contactOutputStream: ContactOutputStream,
        localAddress: ZonaRosaServiceAddress,
        contactsManager: OWSContactsManager,
        tx: DBReadTransaction,
    ) throws {
        let threadFinder = ThreadFinder()
        var threadPositions = [Int64: Int]()
        for (inboxPosition, rowId) in try threadFinder.fetchContactSyncThreadRowIds(tx: tx).enumerated() {
            threadPositions[rowId] = inboxPosition + 1 // Row numbers start from 1.
        }

        let localAccount = localAccountToSync(localAddress: localAddress)
        let otherAccounts = ZonaRosaAccount.anyFetchAll(transaction: tx)
        let zonarosaAccounts = [localAccount] + otherAccounts.sorted(
            by: { ($0.recipientPhoneNumber ?? "") < ($1.recipientPhoneNumber ?? "") },
        )

        // De-duplicate threads by their address. This de-duping works correctly
        // because we no longer allow stale information on TSThreads and removed
        // all existing stale information via removeRedundantPhoneNumbers.
        var seenAddresses = Set<ZonaRosaServiceAddress>()

        let recipientDatabaseTable = DependenciesBridge.shared.recipientDatabaseTable

        for zonarosaAccount in zonarosaAccounts {
            try autoreleasepool {
                guard let phoneNumber = zonarosaAccount.recipientPhoneNumber else {
                    return
                }
                let zonarosaRecipient = recipientDatabaseTable.fetchRecipient(phoneNumber: phoneNumber, transaction: tx)
                guard let zonarosaRecipient else {
                    return
                }
                let contactThread = TSContactThread.getWithContactAddress(zonarosaRecipient.address, transaction: tx)
                let inboxPosition = contactThread?.sqliteRowId.flatMap { threadPositions.removeValue(forKey: $0) }
                try writeContact(
                    to: contactOutputStream,
                    address: zonarosaRecipient.address,
                    contactThread: contactThread,
                    zonarosaAccount: zonarosaAccount,
                    inboxPosition: inboxPosition,
                    tx: tx,
                )
                seenAddresses.insert(zonarosaRecipient.address)
            }
        }

        for (rowId, inboxPosition) in threadPositions.sorted(by: { $0.key < $1.key }) {
            try autoreleasepool {
                guard let contactThread = threadFinder.fetch(rowId: rowId, tx: tx) as? TSContactThread else {
                    return
                }
                guard seenAddresses.insert(contactThread.contactAddress).inserted else {
                    Logger.warn("Skipping duplicate thread for \(contactThread.contactAddress)")
                    return
                }
                try writeContact(
                    to: contactOutputStream,
                    address: contactThread.contactAddress,
                    contactThread: contactThread,
                    zonarosaAccount: nil,
                    inboxPosition: inboxPosition,
                    tx: tx,
                )
            }
        }
    }

    private static func writeContact(
        to contactOutputStream: ContactOutputStream,
        address: ZonaRosaServiceAddress,
        contactThread: TSContactThread?,
        zonarosaAccount: ZonaRosaAccount?,
        inboxPosition: Int?,
        tx: DBReadTransaction,
    ) throws {
        let dmStore = DependenciesBridge.shared.disappearingMessagesConfigurationStore
        let dmConfiguration = contactThread.map { dmStore.fetchOrBuildDefault(for: .thread($0), tx: tx) }

        try contactOutputStream.writeContact(
            aci: address.serviceId as? Aci,
            phoneNumber: address.e164,
            zonarosaAccount: zonarosaAccount,
            disappearingMessagesConfiguration: dmConfiguration,
            inboxPosition: inboxPosition,
        )
    }

    private static func localAccountToSync(localAddress: ZonaRosaServiceAddress) -> ZonaRosaAccount {
        // OWSContactsOutputStream requires all zonarosaAccount to have a contact.
        return ZonaRosaAccount(
            recipientPhoneNumber: localAddress.phoneNumber,
            recipientServiceId: localAddress.serviceId,
            multipleAccountLabelText: nil,
            cnContactId: nil,
            givenName: "",
            familyName: "",
            nickname: "",
            fullName: "",
            contactAvatarHash: nil,
        )
    }
}

private class OWSStreamDelegate: NSObject, StreamDelegate {
    private let _hadError = AtomicBool(false, lock: .sharedGlobal)
    var hadError: Bool { _hadError.get() }

    @objc
    func stream(_ stream: Stream, handle eventCode: Stream.Event) {
        if eventCode == .errorOccurred {
            _hadError.set(true)
        }
    }
}
