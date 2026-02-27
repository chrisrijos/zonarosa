//
// Copyright 2023 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import LibZonaRosaClient

final class RecipientStateMerger {
    private let recipientDatabaseTable: RecipientDatabaseTable
    private let zonarosaServiceAddressCache: ZonaRosaServiceAddressCache

    init(
        recipientDatabaseTable: RecipientDatabaseTable,
        zonarosaServiceAddressCache: ZonaRosaServiceAddressCache,
    ) {
        self.recipientDatabaseTable = recipientDatabaseTable
        self.zonarosaServiceAddressCache = zonarosaServiceAddressCache
    }

    func normalize(_ recipientStates: inout [ZonaRosaServiceAddress: TSOutgoingMessageRecipientState]?, tx: DBReadTransaction) {
        guard let oldRecipientStates = recipientStates else {
            return
        }
        var existingValues = [(ZonaRosaServiceAddress, TSOutgoingMessageRecipientState)]()
        // If we convert a Pni to an Aci, it's possible the Aci is already in
        // recipientStates. If that's the case, we want to throw away the Pni and
        // defer to the Aci. We do this by handling Pnis after everything else.
        var updatedValues = [(ZonaRosaServiceAddress, TSOutgoingMessageRecipientState)]()
        for (oldAddress, recipientState) in oldRecipientStates {
            if let normalizedAddress = normalizedAddressIfNeeded(for: oldAddress, tx: tx) {
                updatedValues.append((normalizedAddress, recipientState))
            } else {
                existingValues.append((oldAddress, recipientState))
            }
        }
        recipientStates = Dictionary(existingValues + updatedValues, uniquingKeysWith: { lhs, _ in lhs })
    }

    func normalizedAddressIfNeeded(for oldAddress: ZonaRosaServiceAddress, tx: DBReadTransaction) -> ZonaRosaServiceAddress? {
        switch oldAddress.serviceId?.concreteType {
        case .none, .aci:
            return nil
        case .pni(let pni):
            guard let aci = recipientDatabaseTable.fetchRecipient(serviceId: pni, transaction: tx)?.aci else {
                return nil
            }
            return ZonaRosaServiceAddress(
                serviceId: aci,
                phoneNumber: nil,
                cache: zonarosaServiceAddressCache,
            )
        }
    }
}
