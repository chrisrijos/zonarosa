//
// Copyright 2023 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import ZonaRosaFfi

public class KEMKeyPair: ClonableHandleOwner<ZonaRosaMutPointerKyberKeyPair>, @unchecked Sendable {
    public static func generate() -> KEMKeyPair {
        return failOnError {
            try invokeFnReturningNativeHandle {
                zonarosa_kyber_key_pair_generate($0)
            }
        }
    }

    override internal class func cloneNativeHandle(
        _ newHandle: inout ZonaRosaMutPointerKyberKeyPair,
        currentHandle: ZonaRosaConstPointerKyberKeyPair
    ) -> ZonaRosaFfiErrorRef? {
        return zonarosa_kyber_key_pair_clone(&newHandle, currentHandle)
    }

    override internal class func destroyNativeHandle(
        _ handle: NonNull<ZonaRosaMutPointerKyberKeyPair>
    ) -> ZonaRosaFfiErrorRef? {
        return zonarosa_kyber_key_pair_destroy(handle.pointer)
    }

    public var publicKey: KEMPublicKey {
        return withNativeHandle { nativeHandle in
            failOnError {
                try invokeFnReturningNativeHandle {
                    zonarosa_kyber_key_pair_get_public_key($0, nativeHandle.const())
                }
            }
        }
    }

    public var secretKey: KEMSecretKey {
        return withNativeHandle { nativeHandle in
            failOnError {
                try invokeFnReturningNativeHandle {
                    zonarosa_kyber_key_pair_get_secret_key($0, nativeHandle.const())
                }
            }
        }
    }
}

extension ZonaRosaMutPointerKyberKeyPair: ZonaRosaMutPointer {
    public typealias ConstPointer = ZonaRosaConstPointerKyberKeyPair

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

extension ZonaRosaConstPointerKyberKeyPair: ZonaRosaConstPointer {
    public func toOpaque() -> OpaquePointer? {
        self.raw
    }
}

public class KEMPublicKey: ClonableHandleOwner<ZonaRosaMutPointerKyberPublicKey>, @unchecked Sendable {
    public convenience init<Bytes: ContiguousBytes>(_ bytes: Bytes) throws {
        let handle = try bytes.withUnsafeBorrowedBuffer { bytes in
            try invokeFnReturningValueByPointer(.init()) {
                zonarosa_kyber_public_key_deserialize($0, bytes)
            }
        }
        self.init(owned: NonNull(handle)!)
    }

    override internal class func cloneNativeHandle(
        _ newHandle: inout ZonaRosaMutPointerKyberPublicKey,
        currentHandle: ZonaRosaConstPointerKyberPublicKey
    ) -> ZonaRosaFfiErrorRef? {
        return zonarosa_kyber_public_key_clone(&newHandle, currentHandle)
    }

    override internal class func destroyNativeHandle(
        _ handle: NonNull<ZonaRosaMutPointerKyberPublicKey>
    ) -> ZonaRosaFfiErrorRef? {
        return zonarosa_kyber_public_key_destroy(handle.pointer)
    }

    public func serialize() -> Data {
        return withNativeHandle { nativeHandle in
            failOnError {
                try invokeFnReturningData {
                    zonarosa_kyber_public_key_serialize($0, nativeHandle.const())
                }
            }
        }
    }
}

extension ZonaRosaMutPointerKyberPublicKey: ZonaRosaMutPointer {
    public typealias ConstPointer = ZonaRosaConstPointerKyberPublicKey

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

extension ZonaRosaConstPointerKyberPublicKey: ZonaRosaConstPointer {
    public func toOpaque() -> OpaquePointer? {
        self.raw
    }
}

extension KEMPublicKey: Equatable {
    public static func == (lhs: KEMPublicKey, rhs: KEMPublicKey) -> Bool {
        return failOnError {
            try withAllBorrowed(lhs, rhs) { lHandle, rHandle in
                try invokeFnReturningBool {
                    zonarosa_kyber_public_key_equals($0, lHandle.const(), rHandle.const())
                }
            }
        }
    }
}

public class KEMSecretKey: ClonableHandleOwner<ZonaRosaMutPointerKyberSecretKey>, @unchecked Sendable {
    public convenience init<Bytes: ContiguousBytes>(_ bytes: Bytes) throws {
        let handle = try bytes.withUnsafeBorrowedBuffer { bytes in
            try invokeFnReturningValueByPointer(.init()) {
                zonarosa_kyber_secret_key_deserialize($0, bytes)
            }
        }
        self.init(owned: NonNull(handle)!)
    }

    override internal class func cloneNativeHandle(
        _ newHandle: inout ZonaRosaMutPointerKyberSecretKey,
        currentHandle: ZonaRosaConstPointerKyberSecretKey
    ) -> ZonaRosaFfiErrorRef? {
        return zonarosa_kyber_secret_key_clone(&newHandle, currentHandle)
    }

    override internal class func destroyNativeHandle(
        _ handle: NonNull<ZonaRosaMutPointerKyberSecretKey>
    ) -> ZonaRosaFfiErrorRef? {
        return zonarosa_kyber_secret_key_destroy(handle.pointer)
    }

    public func serialize() -> Data {
        return withNativeHandle { nativeHandle in
            failOnError {
                try invokeFnReturningData {
                    zonarosa_kyber_secret_key_serialize($0, nativeHandle.const())
                }
            }
        }
    }
}

extension ZonaRosaMutPointerKyberSecretKey: ZonaRosaMutPointer {
    public typealias ConstPointer = ZonaRosaConstPointerKyberSecretKey

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

extension ZonaRosaConstPointerKyberSecretKey: ZonaRosaConstPointer {
    public func toOpaque() -> OpaquePointer? {
        self.raw
    }
}
