//
// Copyright 2020-2022 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import ZonaRosaFfi

public class PublicKey: ClonableHandleOwner<ZonaRosaMutPointerPublicKey>, @unchecked Sendable, Equatable {
    public convenience init<Bytes: ContiguousBytes>(_ bytes: Bytes) throws {
        let handle = try bytes.withUnsafeBorrowedBuffer { bytes in
            try invokeFnReturningValueByPointer(.init()) {
                zonarosa_publickey_deserialize($0, bytes)
            }
        }
        self.init(owned: NonNull(handle)!)
    }

    override internal class func destroyNativeHandle(_ handle: NonNull<ZonaRosaMutPointerPublicKey>) -> ZonaRosaFfiErrorRef?
    {
        return zonarosa_publickey_destroy(handle.pointer)
    }

    override internal class func cloneNativeHandle(
        _ newHandle: inout ZonaRosaMutPointerPublicKey,
        currentHandle: ZonaRosaConstPointerPublicKey
    ) -> ZonaRosaFfiErrorRef? {
        return zonarosa_publickey_clone(&newHandle, currentHandle)
    }

    public var keyBytes: Data {
        return withNativeHandle { nativeHandle in
            failOnError {
                try invokeFnReturningData {
                    zonarosa_publickey_get_public_key_bytes($0, nativeHandle.const())
                }
            }
        }
    }

    public func serialize() -> Data {
        return withNativeHandle { nativeHandle in
            failOnError {
                try invokeFnReturningData {
                    zonarosa_publickey_serialize($0, nativeHandle.const())
                }
            }
        }
    }

    public func verifySignature(message: some ContiguousBytes, signature: some ContiguousBytes) throws -> Bool {
        return try withAllBorrowed(self, .bytes(message), .bytes(signature)) {
            nativeHandle,
            messageBuffer,
            signatureBuffer in
            try invokeFnReturningBool {
                zonarosa_publickey_verify($0, nativeHandle.const(), messageBuffer, signatureBuffer)
            }
        }
    }

    /// Seals a message so only the holder of the private key can decrypt it.
    ///
    /// Uses HPKE ([RFC 9180][]). The output will include a type byte indicating the chosen
    /// algorithms and ciphertext layout. The `info` parameter should typically be a static value
    /// describing the purpose of the message, while `associatedData` can be used to restrict
    /// successful decryption beyond holding the private key.
    ///
    /// - SeeAlso ``PrivateKey/open(_:info:associatedData:)-(_,ContiguousBytes,_)``
    ///
    /// [RFC 9180]: https://www.rfc-editor.org/rfc/rfc9180.html
    public func seal(
        _ message: some ContiguousBytes,
        info: some ContiguousBytes,
        associatedData: some ContiguousBytes = []
    ) -> Data {
        failOnError {
            try withAllBorrowed(self, .bytes(message), .bytes(info), .bytes(associatedData)) {
                nativeHandle,
                messageBuffer,
                infoBuffer,
                aadBuffer in
                try invokeFnReturningData {
                    zonarosa_publickey_hpke_seal($0, nativeHandle.const(), messageBuffer, infoBuffer, aadBuffer)
                }
            }
        }
    }

    /// Convenience overload for ``seal(_:info:associatedData:)-(_,ContiguousBytes,_)``, using the UTF-8 bytes of `info`.
    public func seal(
        _ message: some ContiguousBytes,
        info: String,
        associatedData: some ContiguousBytes = []
    ) -> Data {
        var info = info
        return info.withUTF8 {
            seal(message, info: $0, associatedData: associatedData)
        }
    }

    public static func == (lhs: PublicKey, rhs: PublicKey) -> Bool {
        return failOnError {
            try withAllBorrowed(lhs, rhs) { lhsHandle, rhsHandle in
                try invokeFnReturningBool {
                    zonarosa_publickey_equals($0, lhsHandle.const(), rhsHandle.const())
                }
            }
        }
    }
}

extension ZonaRosaMutPointerPublicKey: ZonaRosaMutPointer {
    public typealias ConstPointer = ZonaRosaConstPointerPublicKey

    public init(untyped: OpaquePointer?) {
        self.init(raw: untyped)
    }

    public func toOpaque() -> OpaquePointer? {
        self.raw
    }

    public func const() -> ZonaRosaConstPointerPublicKey {
        return ZonaRosaConstPointerPublicKey(raw: self.raw)
    }
}

extension ZonaRosaConstPointerPublicKey: ZonaRosaConstPointer {
    public func toOpaque() -> OpaquePointer? {
        self.raw
    }
}
