//
// Copyright 2020-2022 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import ZonaRosaFfi

public func zonarosaEncrypt<Bytes: ContiguousBytes>(
    message: Bytes,
    for address: ProtocolAddress,
    sessionStore: SessionStore,
    identityStore: IdentityKeyStore,
    now: Date = Date(),
    context: StoreContext
) throws -> CiphertextMessage {
    return try withAllBorrowed(address, .bytes(message)) { addressHandle, messageBuffer in
        try withSessionStore(sessionStore, context) { ffiSessionStore in
            try withIdentityKeyStore(identityStore, context) { ffiIdentityStore in
                try invokeFnReturningNativeHandle {
                    zonarosa_encrypt_message(
                        $0,
                        messageBuffer,
                        addressHandle.const(),
                        ffiSessionStore,
                        ffiIdentityStore,
                        UInt64(now.timeIntervalSince1970 * 1000)
                    )
                }
            }
        }
    }
}

public func zonarosaDecrypt(
    message: ZonaRosaMessage,
    from address: ProtocolAddress,
    sessionStore: SessionStore,
    identityStore: IdentityKeyStore,
    context: StoreContext
) throws -> Data {
    return try withAllBorrowed(message, address) { messageHandle, addressHandle in
        try withSessionStore(sessionStore, context) { ffiSessionStore in
            try withIdentityKeyStore(identityStore, context) { ffiIdentityStore in
                try invokeFnReturningData {
                    zonarosa_decrypt_message(
                        $0,
                        messageHandle.const(),
                        addressHandle.const(),
                        ffiSessionStore,
                        ffiIdentityStore
                    )
                }
            }
        }
    }
}

public func zonarosaDecryptPreKey(
    message: PreKeyZonaRosaMessage,
    from address: ProtocolAddress,
    sessionStore: SessionStore,
    identityStore: IdentityKeyStore,
    preKeyStore: PreKeyStore,
    signedPreKeyStore: SignedPreKeyStore,
    kyberPreKeyStore: KyberPreKeyStore,
    context: StoreContext
) throws -> Data {
    return try withAllBorrowed(message, address) { messageHandle, addressHandle in
        try withSessionStore(sessionStore, context) { ffiSessionStore in
            try withIdentityKeyStore(identityStore, context) { ffiIdentityStore in
                try withPreKeyStore(preKeyStore, context) { ffiPreKeyStore in
                    try withSignedPreKeyStore(signedPreKeyStore, context) { ffiSignedPreKeyStore in
                        try withKyberPreKeyStore(kyberPreKeyStore, context) { ffiKyberPreKeyStore in
                            try invokeFnReturningData {
                                zonarosa_decrypt_pre_key_message(
                                    $0,
                                    messageHandle.const(),
                                    addressHandle.const(),
                                    ffiSessionStore,
                                    ffiIdentityStore,
                                    ffiPreKeyStore,
                                    ffiSignedPreKeyStore,
                                    ffiKyberPreKeyStore
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

public func processPreKeyBundle(
    _ bundle: PreKeyBundle,
    for address: ProtocolAddress,
    sessionStore: SessionStore,
    identityStore: IdentityKeyStore,
    now: Date = Date(),
    context: StoreContext
) throws {
    return try withAllBorrowed(bundle, address) { bundleHandle, addressHandle in
        try withSessionStore(sessionStore, context) { ffiSessionStore in
            try withIdentityKeyStore(identityStore, context) { ffiIdentityStore in
                try checkError(
                    zonarosa_process_prekey_bundle(
                        bundleHandle.const(),
                        addressHandle.const(),
                        ffiSessionStore,
                        ffiIdentityStore,
                        UInt64(now.timeIntervalSince1970 * 1000)
                    )
                )
            }
        }
    }
}

public func groupEncrypt<Bytes: ContiguousBytes>(
    _ message: Bytes,
    from sender: ProtocolAddress,
    distributionId: UUID,
    store: SenderKeyStore,
    context: StoreContext
) throws -> CiphertextMessage {
    return try withAllBorrowed(sender, .bytes(message), distributionId) { senderHandle, messageBuffer, distributionId in
        try withSenderKeyStore(store, context) { ffiStore in
            try invokeFnReturningNativeHandle {
                zonarosa_group_encrypt_message($0, senderHandle.const(), distributionId, messageBuffer, ffiStore)
            }
        }
    }
}

public func groupDecrypt<Bytes: ContiguousBytes>(
    _ message: Bytes,
    from sender: ProtocolAddress,
    store: SenderKeyStore,
    context: StoreContext
) throws -> Data {
    return try withAllBorrowed(sender, .bytes(message)) { senderHandle, messageBuffer in
        try withSenderKeyStore(store, context) { ffiStore in
            try invokeFnReturningData {
                zonarosa_group_decrypt_message($0, senderHandle.const(), messageBuffer, ffiStore)
            }
        }
    }
}

public func processSenderKeyDistributionMessage(
    _ message: SenderKeyDistributionMessage,
    from sender: ProtocolAddress,
    store: SenderKeyStore,
    context: StoreContext
) throws {
    return try withAllBorrowed(sender, message) { senderHandle, messageHandle in
        try withSenderKeyStore(store, context) {
            try checkError(
                zonarosa_process_sender_key_distribution_message(
                    senderHandle.const(),
                    messageHandle.const(),
                    $0
                )
            )
        }
    }
}
