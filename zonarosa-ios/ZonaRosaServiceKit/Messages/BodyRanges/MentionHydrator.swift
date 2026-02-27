//
// Copyright 2023 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
public import LibZonaRosaClient

public enum MentionHydrationOption {
    /// Do not hydrate the mention; this leaves the string as it was in the original,
    /// which we want to do e.g. when forwarding a message with mentions from one
    /// thread context to another, where we hydrate the mentions of members not in
    /// the destination, but preserve mentions of shared members fully intact.
    case preserveMention
    /// Replace the mention range with the populated display name.
    case hydrate(String)
}

public typealias MentionHydrator = (Aci) -> MentionHydrationOption

public class ContactsMentionHydrator {

    public static func mentionHydrator(
        excludedAcis: Set<Aci>? = nil,
        transaction: DBReadTransaction,
    ) -> MentionHydrator {
        return { mentionAci in
            if excludedAcis?.contains(mentionAci) == true {
                return .preserveMention
            }
            return .hydrate(
                Self.hydrateMention(with: mentionAci, transaction: transaction).1,
            )
        }
    }

    public static func hydrateMention(
        with mentionAci: Aci,
        transaction: DBReadTransaction,
    ) -> (ZonaRosaServiceAddress, String) {
        let address = ZonaRosaServiceAddress(mentionAci)
        let displayName = SSKEnvironment.shared.contactManagerRef.displayName(
            for: address,
            tx: transaction,
        ).resolvedValue()
        return (address, displayName)
    }
}
