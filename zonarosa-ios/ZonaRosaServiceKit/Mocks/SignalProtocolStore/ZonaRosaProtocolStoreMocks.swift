//
// Copyright 2023 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

#if TESTABLE_BUILD

import Foundation
import LibZonaRosaClient

extension ZonaRosaProtocolStore {
    static func mock(identity: OWSIdentity, preKeyStore: PreKeyStore, recipientIdFinder: RecipientIdFinder, sessionStore: SessionStore) -> Self {
        return ZonaRosaProtocolStore(
            sessionStore: SessionManagerForIdentity(identity: identity, recipientIdFinder: recipientIdFinder, sessionStore: sessionStore),
            preKeyStore: PreKeyStoreImpl(for: identity, preKeyStore: preKeyStore),
            signedPreKeyStore: SignedPreKeyStoreImpl(for: identity, preKeyStore: preKeyStore),
            kyberPreKeyStore: KyberPreKeyStoreImpl(for: identity, dateProvider: Date.provider, preKeyStore: preKeyStore),
        )
    }
}

#endif
