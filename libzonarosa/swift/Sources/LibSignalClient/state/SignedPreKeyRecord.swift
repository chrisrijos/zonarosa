//
// Copyright 2020-2022 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import ZonaRosaFfi

public class SignedPreKeyRecord: ClonableHandleOwner<ZonaRosaMutPointerSignedPreKeyRecord> {
    override internal class func destroyNativeHandle(
        _ handle: NonNull<ZonaRosaMutPointerSignedPreKeyRecord>
    ) -> ZonaRosaFfiErrorRef? {
        return zonarosa_signed_pre_key_record_destroy(handle.pointer)
    }

    override internal class func cloneNativeHandle(
        _ newHandle: inout ZonaRosaMutPointerSignedPreKeyRecord,
        currentHandle: ZonaRosaConstPointerSignedPreKeyRecord
    ) -> ZonaRosaFfiErrorRef? {
        return zonarosa_signed_pre_key_record_clone(&newHandle, currentHandle)
    }

    public convenience init<Bytes: ContiguousBytes>(bytes: Bytes) throws {
        let handle = try bytes.withUnsafeBorrowedBuffer { bytes in
            try invokeFnReturningValueByPointer(.init()) {
                zonarosa_signed_pre_key_record_deserialize($0, bytes)
            }
        }
        self.init(owned: NonNull(handle)!)
    }

    public convenience init<Bytes: ContiguousBytes>(
        id: UInt32,
        timestamp: UInt64,
        privateKey: PrivateKey,
        signature: Bytes
    ) throws {
        let publicKey = privateKey.publicKey
        let result = try withAllBorrowed(publicKey, privateKey, .bytes(signature)) {
            publicKeyHandle,
            privateKeyHandle,
            signature in
            try invokeFnReturningValueByPointer(.init()) {
                zonarosa_signed_pre_key_record_new(
                    $0,
                    id,
                    timestamp,
                    publicKeyHandle.const(),
                    privateKeyHandle.const(),
                    signature
                )
            }
        }
        self.init(owned: NonNull(result)!)
    }

    public func serialize() -> Data {
        return withNativeHandle { nativeHandle in
            failOnError {
                try invokeFnReturningData {
                    zonarosa_signed_pre_key_record_serialize($0, nativeHandle.const())
                }
            }
        }
    }

    public var id: UInt32 {
        return withNativeHandle { nativeHandle in
            failOnError {
                try invokeFnReturningInteger {
                    zonarosa_signed_pre_key_record_get_id($0, nativeHandle.const())
                }
            }
        }
    }

    public var timestamp: UInt64 {
        return withNativeHandle { nativeHandle in
            failOnError {
                try invokeFnReturningInteger {
                    zonarosa_signed_pre_key_record_get_timestamp($0, nativeHandle.const())
                }
            }
        }
    }

    public func publicKey() throws -> PublicKey {
        return try withNativeHandle { nativeHandle in
            try invokeFnReturningNativeHandle {
                zonarosa_signed_pre_key_record_get_public_key($0, nativeHandle.const())
            }
        }
    }

    public func privateKey() throws -> PrivateKey {
        return try withNativeHandle { nativeHandle in
            try invokeFnReturningNativeHandle {
                zonarosa_signed_pre_key_record_get_private_key($0, nativeHandle.const())
            }
        }
    }

    public var signature: Data {
        return withNativeHandle { nativeHandle in
            failOnError {
                try invokeFnReturningData {
                    zonarosa_signed_pre_key_record_get_signature($0, nativeHandle.const())
                }
            }
        }
    }
}

extension ZonaRosaMutPointerSignedPreKeyRecord: ZonaRosaMutPointer {
    public typealias ConstPointer = ZonaRosaConstPointerSignedPreKeyRecord

    public init(untyped: OpaquePointer?) {
        self.init(raw: untyped)
    }

    public func toOpaque() -> OpaquePointer? {
        self.raw
    }

    public func const() -> Self.ConstPointer {
        Self.ConstPointer(raw: self.raw)
    }
}

extension ZonaRosaConstPointerSignedPreKeyRecord: ZonaRosaConstPointer {
    public func toOpaque() -> OpaquePointer? {
        self.raw
    }
}
