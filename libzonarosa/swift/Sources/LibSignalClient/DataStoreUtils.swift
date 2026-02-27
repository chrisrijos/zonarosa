//
// Copyright 2020-2021 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import ZonaRosaFfi

internal func withIdentityKeyStore<Result>(
    _ store: IdentityKeyStore,
    _ context: StoreContext,
    _ body: (ZonaRosaConstPointerFfiIdentityKeyStoreStruct) throws -> Result
) throws -> Result {
    func ffiShimGetIdentityPrivateKey(
        storeCtx: UnsafeMutableRawPointer?,
        keyp: UnsafeMutablePointer<ZonaRosaMutPointerPrivateKey>?
    ) -> Int32 {
        let storeContext = storeCtx!.assumingMemoryBound(
            to: ErrorHandlingContext<(IdentityKeyStore, StoreContext)>.self
        )
        return storeContext.pointee.catchCallbackErrors { store, context in
            var privateKey = try store.identityKeyPair(context: context).privateKey
            keyp!.pointee = try cloneOrTakeHandle(from: &privateKey)
        }
    }

    func ffiShimGetLocalRegistrationId(
        storeCtx: UnsafeMutableRawPointer?,
        idp: UnsafeMutablePointer<UInt32>?
    ) -> Int32 {
        let storeContext = storeCtx!.assumingMemoryBound(
            to: ErrorHandlingContext<(IdentityKeyStore, StoreContext)>.self
        )
        return storeContext.pointee.catchCallbackErrors { store, context in
            let id = try store.localRegistrationId(context: context)
            idp!.pointee = id
        }
    }

    func ffiShimSaveIdentity(
        storeCtx: UnsafeMutableRawPointer?,
        result: UnsafeMutablePointer<UInt8>?,
        address: ZonaRosaMutPointerProtocolAddress,
        public_key: ZonaRosaMutPointerPublicKey
    ) -> Int32 {
        let storeContext = storeCtx!.assumingMemoryBound(
            to: ErrorHandlingContext<(IdentityKeyStore, StoreContext)>.self
        )
        return storeContext.pointee.catchCallbackErrors { store, context in
            let address = ProtocolAddress(owned: NonNull(address)!)
            let public_key = PublicKey(owned: NonNull(public_key)!)
            let identity = IdentityKey(publicKey: public_key)
            result!.pointee =
                switch try store.saveIdentity(identity, for: address, context: context) {
                case .newOrUnchanged: UInt8(ZonaRosaIdentityChangeNewOrUnchanged.rawValue)
                case .replacedExisting: UInt8(ZonaRosaIdentityChangeReplacedExisting.rawValue)
                }
        }
    }

    func ffiShimGetIdentity(
        storeCtx: UnsafeMutableRawPointer?,
        public_key: UnsafeMutablePointer<ZonaRosaMutPointerPublicKey>?,
        address: ZonaRosaMutPointerProtocolAddress
    ) -> Int32 {
        let storeContext = storeCtx!.assumingMemoryBound(
            to: ErrorHandlingContext<(IdentityKeyStore, StoreContext)>.self
        )
        return storeContext.pointee.catchCallbackErrors { store, context in
            let address = ProtocolAddress(owned: NonNull(address)!)
            if let pk = try store.identity(for: address, context: context) {
                var publicKey = pk.publicKey
                public_key!.pointee = try cloneOrTakeHandle(from: &publicKey)
            } else {
                public_key!.pointee = ZonaRosaMutPointerPublicKey()
            }
        }
    }

    func ffiShimIsTrustedIdentity(
        storeCtx: UnsafeMutableRawPointer?,
        result: UnsafeMutablePointer<Bool>?,
        address: ZonaRosaMutPointerProtocolAddress,
        public_key: ZonaRosaMutPointerPublicKey,
        raw_direction: UInt32
    ) -> Int32 {
        let storeContext = storeCtx!.assumingMemoryBound(
            to: ErrorHandlingContext<(IdentityKeyStore, StoreContext)>.self
        )
        return storeContext.pointee.catchCallbackErrors { store, context in
            let address = ProtocolAddress(owned: NonNull(address)!)
            let public_key = PublicKey(owned: NonNull(public_key)!)
            let direction: Direction
            switch ZonaRosaDirection(raw_direction) {
            case ZonaRosaDirectionSending:
                direction = .sending
            case ZonaRosaDirectionReceiving:
                direction = .receiving
            default:
                throw ZonaRosaError.internalError("unexpected direction value \(raw_direction)")
            }
            let identity = IdentityKey(publicKey: public_key)
            result!.pointee = try store.isTrustedIdentity(
                identity,
                for: address,
                direction: direction,
                context: context
            )
        }
    }

    return try rethrowCallbackErrors((store, context)) {
        var ffiStore = ZonaRosaIdentityKeyStore(
            ctx: $0,
            get_local_identity_private_key: ffiShimGetIdentityPrivateKey,
            get_local_registration_id: ffiShimGetLocalRegistrationId,
            get_identity_key: ffiShimGetIdentity,
            save_identity_key: ffiShimSaveIdentity,
            is_trusted_identity: ffiShimIsTrustedIdentity,
            destroy: { _ in }
        )
        return try withUnsafePointer(to: &ffiStore) {
            try body(ZonaRosaConstPointerFfiIdentityKeyStoreStruct(raw: $0))
        }
    }
}

internal func withPreKeyStore<Result>(
    _ store: PreKeyStore,
    _ context: StoreContext,
    _ body: (ZonaRosaConstPointerFfiPreKeyStoreStruct) throws -> Result
) throws -> Result {
    func ffiShimStorePreKey(
        storeCtx: UnsafeMutableRawPointer?,
        id: UInt32,
        record: ZonaRosaMutPointerPreKeyRecord
    ) -> Int32 {
        let storeContext = storeCtx!.assumingMemoryBound(to: ErrorHandlingContext<(PreKeyStore, StoreContext)>.self)
        return storeContext.pointee.catchCallbackErrors { store, context in
            let record = PreKeyRecord(owned: NonNull(record)!)
            try store.storePreKey(record, id: id, context: context)
        }
    }

    func ffiShimLoadPreKey(
        storeCtx: UnsafeMutableRawPointer?,
        recordp: UnsafeMutablePointer<ZonaRosaMutPointerPreKeyRecord>?,
        id: UInt32
    ) -> Int32 {
        let storeContext = storeCtx!.assumingMemoryBound(to: ErrorHandlingContext<(PreKeyStore, StoreContext)>.self)
        return storeContext.pointee.catchCallbackErrors { store, context in
            var record = try store.loadPreKey(id: id, context: context)
            recordp!.pointee = try cloneOrTakeHandle(from: &record)
        }
    }

    func ffiShimRemovePreKey(
        storeCtx: UnsafeMutableRawPointer?,
        id: UInt32
    ) -> Int32 {
        let storeContext = storeCtx!.assumingMemoryBound(to: ErrorHandlingContext<(PreKeyStore, StoreContext)>.self)
        return storeContext.pointee.catchCallbackErrors { store, context in
            try store.removePreKey(id: id, context: context)
        }
    }

    return try rethrowCallbackErrors((store, context)) {
        var ffiStore = ZonaRosaPreKeyStore(
            ctx: $0,
            load_pre_key: ffiShimLoadPreKey,
            store_pre_key: ffiShimStorePreKey,
            remove_pre_key: ffiShimRemovePreKey,
            destroy: { _ in }
        )
        return try withUnsafePointer(to: &ffiStore) {
            try body(ZonaRosaConstPointerFfiPreKeyStoreStruct(raw: $0))
        }
    }
}

internal func withSignedPreKeyStore<Result>(
    _ store: SignedPreKeyStore,
    _ context: StoreContext,
    _ body: (ZonaRosaConstPointerFfiSignedPreKeyStoreStruct) throws -> Result
) throws -> Result {
    func ffiShimStoreSignedPreKey(
        storeCtx: UnsafeMutableRawPointer?,
        id: UInt32,
        record: ZonaRosaMutPointerSignedPreKeyRecord
    ) -> Int32 {
        let storeContext = storeCtx!.assumingMemoryBound(
            to: ErrorHandlingContext<(SignedPreKeyStore, StoreContext)>.self
        )
        return storeContext.pointee.catchCallbackErrors { store, context in
            let record = SignedPreKeyRecord(owned: NonNull(record)!)
            try store.storeSignedPreKey(record, id: id, context: context)
        }
    }

    func ffiShimLoadSignedPreKey(
        storeCtx: UnsafeMutableRawPointer?,
        recordp: UnsafeMutablePointer<ZonaRosaMutPointerSignedPreKeyRecord>?,
        id: UInt32
    ) -> Int32 {
        let storeContext = storeCtx!.assumingMemoryBound(
            to: ErrorHandlingContext<(SignedPreKeyStore, StoreContext)>.self
        )
        return storeContext.pointee.catchCallbackErrors { store, context in
            var record = try store.loadSignedPreKey(id: id, context: context)
            recordp!.pointee = try cloneOrTakeHandle(from: &record)
        }
    }

    return try rethrowCallbackErrors((store, context)) {
        var ffiStore = ZonaRosaSignedPreKeyStore(
            ctx: $0,
            load_signed_pre_key: ffiShimLoadSignedPreKey,
            store_signed_pre_key: ffiShimStoreSignedPreKey,
            destroy: { _ in }
        )
        return try withUnsafePointer(to: &ffiStore) {
            try body(ZonaRosaConstPointerFfiSignedPreKeyStoreStruct(raw: $0))
        }
    }
}

internal func withKyberPreKeyStore<Result>(
    _ store: KyberPreKeyStore,
    _ context: StoreContext,
    _ body: (ZonaRosaConstPointerFfiKyberPreKeyStoreStruct) throws -> Result
) throws -> Result {
    func ffiShimStoreKyberPreKey(
        storeCtx: UnsafeMutableRawPointer?,
        id: UInt32,
        record: ZonaRosaMutPointerKyberPreKeyRecord
    ) -> Int32 {
        let storeContext = storeCtx!.assumingMemoryBound(
            to: ErrorHandlingContext<(KyberPreKeyStore, StoreContext)>.self
        )
        return storeContext.pointee.catchCallbackErrors { store, context in
            let record = KyberPreKeyRecord(owned: NonNull(record)!)
            try store.storeKyberPreKey(record, id: id, context: context)
        }
    }

    func ffiShimLoadKyberPreKey(
        storeCtx: UnsafeMutableRawPointer?,
        recordp: UnsafeMutablePointer<ZonaRosaMutPointerKyberPreKeyRecord>?,
        id: UInt32
    ) -> Int32 {
        let storeContext = storeCtx!.assumingMemoryBound(
            to: ErrorHandlingContext<(KyberPreKeyStore, StoreContext)>.self
        )
        return storeContext.pointee.catchCallbackErrors { store, context in
            var record = try store.loadKyberPreKey(id: id, context: context)
            recordp!.pointee = try cloneOrTakeHandle(from: &record)
        }
    }

    func ffiShimMarkKyberPreKeyUsed(
        storeCtx: UnsafeMutableRawPointer?,
        id: UInt32,
        signedPreKeyId: UInt32,
        baseKey: ZonaRosaMutPointerPublicKey,
    ) -> Int32 {
        let storeContext = storeCtx!.assumingMemoryBound(
            to: ErrorHandlingContext<(KyberPreKeyStore, StoreContext)>.self
        )
        return storeContext.pointee.catchCallbackErrors { store, context in
            let baseKey = PublicKey(owned: NonNull(baseKey)!)
            try store.markKyberPreKeyUsed(id: id, signedPreKeyId: signedPreKeyId, baseKey: baseKey, context: context)
        }
    }

    return try rethrowCallbackErrors((store, context)) {
        var ffiStore = ZonaRosaKyberPreKeyStore(
            ctx: $0,
            load_kyber_pre_key: ffiShimLoadKyberPreKey,
            store_kyber_pre_key: ffiShimStoreKyberPreKey,
            mark_kyber_pre_key_used: ffiShimMarkKyberPreKeyUsed,
            destroy: { _ in }
        )
        return try withUnsafePointer(to: &ffiStore) {
            try body(ZonaRosaConstPointerFfiKyberPreKeyStoreStruct(raw: $0))
        }
    }
}

internal func withSessionStore<Result>(
    _ store: SessionStore,
    _ context: StoreContext,
    _ body: (ZonaRosaConstPointerFfiSessionStoreStruct) throws -> Result
) throws -> Result {
    func ffiShimStoreSession(
        storeCtx: UnsafeMutableRawPointer?,
        address: ZonaRosaMutPointerProtocolAddress,
        record: ZonaRosaMutPointerSessionRecord
    ) -> Int32 {
        let storeContext = storeCtx!.assumingMemoryBound(to: ErrorHandlingContext<(SessionStore, StoreContext)>.self)
        return storeContext.pointee.catchCallbackErrors { store, context in
            let address = ProtocolAddress(owned: NonNull(address)!)
            let record = SessionRecord(owned: NonNull(record)!)
            try store.storeSession(record, for: address, context: context)
        }
    }

    func ffiShimLoadSession(
        storeCtx: UnsafeMutableRawPointer?,
        recordp: UnsafeMutablePointer<ZonaRosaMutPointerSessionRecord>?,
        address: ZonaRosaMutPointerProtocolAddress
    ) -> Int32 {
        let storeContext = storeCtx!.assumingMemoryBound(to: ErrorHandlingContext<(SessionStore, StoreContext)>.self)
        return storeContext.pointee.catchCallbackErrors { store, context in
            let address = ProtocolAddress(owned: NonNull(address)!)
            if var record = try store.loadSession(for: address, context: context) {
                recordp!.pointee = try cloneOrTakeHandle(from: &record)
            } else {
                recordp!.pointee = ZonaRosaMutPointerSessionRecord()
            }
        }
    }

    return try rethrowCallbackErrors((store, context)) {
        var ffiStore = ZonaRosaSessionStore(
            ctx: $0,
            load_session: ffiShimLoadSession,
            store_session: ffiShimStoreSession,
            destroy: { _ in }
        )
        return try withUnsafePointer(to: &ffiStore) {
            try body(ZonaRosaConstPointerFfiSessionStoreStruct(raw: $0))
        }
    }
}

internal func withSenderKeyStore<Result>(
    _ store: SenderKeyStore,
    _ context: StoreContext,
    _ body: (ZonaRosaConstPointerFfiSenderKeyStoreStruct) throws -> Result
) rethrows -> Result {
    func ffiShimStoreSenderKey(
        storeCtx: UnsafeMutableRawPointer?,
        sender: ZonaRosaMutPointerProtocolAddress,
        distributionId: ZonaRosaUuid,
        record: ZonaRosaMutPointerSenderKeyRecord
    ) -> Int32 {
        let storeContext = storeCtx!.assumingMemoryBound(to: ErrorHandlingContext<(SenderKeyStore, StoreContext)>.self)
        return storeContext.pointee.catchCallbackErrors { store, context in
            let sender = ProtocolAddress(owned: NonNull(sender)!)
            let distributionId = UUID(uuid: distributionId.bytes)
            let record = SenderKeyRecord(owned: NonNull(record)!)
            try store.storeSenderKey(from: sender, distributionId: distributionId, record: record, context: context)
        }
    }

    func ffiShimLoadSenderKey(
        storeCtx: UnsafeMutableRawPointer?,
        recordp: UnsafeMutablePointer<ZonaRosaMutPointerSenderKeyRecord>?,
        sender: ZonaRosaMutPointerProtocolAddress,
        distributionId: ZonaRosaUuid,
    ) -> Int32 {
        let storeContext = storeCtx!.assumingMemoryBound(to: ErrorHandlingContext<(SenderKeyStore, StoreContext)>.self)
        return storeContext.pointee.catchCallbackErrors { store, context in
            let sender = ProtocolAddress(owned: NonNull(sender)!)
            let distributionId = UUID(uuid: distributionId.bytes)
            if var record = try store.loadSenderKey(from: sender, distributionId: distributionId, context: context) {
                recordp!.pointee = try cloneOrTakeHandle(from: &record)
            } else {
                recordp!.pointee = ZonaRosaMutPointerSenderKeyRecord()
            }
        }
    }

    return try rethrowCallbackErrors((store, context)) {
        var ffiStore = ZonaRosaSenderKeyStore(
            ctx: $0,
            load_sender_key: ffiShimLoadSenderKey,
            store_sender_key: ffiShimStoreSenderKey,
            destroy: { _ in }
        )
        return try withUnsafePointer(to: &ffiStore) {
            try body(ZonaRosaConstPointerFfiSenderKeyStoreStruct(raw: $0))
        }
    }
}
