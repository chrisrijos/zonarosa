//
// Copyright 2021-2022 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import ZonaRosaFfi

public class ServerCertificate: NativeHandleOwner<ZonaRosaMutPointerServerCertificate>, @unchecked Sendable {
    public convenience init<Bytes: ContiguousBytes>(_ bytes: Bytes) throws {
        let handle = try bytes.withUnsafeBorrowedBuffer { bytes in
            try invokeFnReturningValueByPointer(.init()) {
                zonarosa_server_certificate_deserialize($0, bytes)
            }
        }
        self.init(owned: NonNull(handle)!)
    }

    // For testing
    public convenience init(keyId: UInt32, publicKey: PublicKey, trustRoot: PrivateKey) throws {
        let result = try withAllBorrowed(publicKey, trustRoot) { publicKeyHandle, trustRootHandle in
            try invokeFnReturningValueByPointer(.init()) {
                zonarosa_server_certificate_new($0, keyId, publicKeyHandle.const(), trustRootHandle.const())
            }
        }
        self.init(owned: NonNull(result)!)
    }

    override internal class func destroyNativeHandle(
        _ handle: NonNull<ZonaRosaMutPointerServerCertificate>
    ) -> ZonaRosaFfiErrorRef? {
        return zonarosa_server_certificate_destroy(handle.pointer)
    }

    public var keyId: UInt32 {
        return withNativeHandle { nativeHandle in
            failOnError {
                try invokeFnReturningInteger {
                    zonarosa_server_certificate_get_key_id($0, nativeHandle.const())
                }
            }
        }
    }

    public func serialize() -> Data {
        return withNativeHandle { nativeHandle in
            failOnError {
                try invokeFnReturningData {
                    zonarosa_server_certificate_get_serialized($0, nativeHandle.const())
                }
            }
        }
    }

    public var certificateBytes: Data {
        return withNativeHandle { nativeHandle in
            failOnError {
                try invokeFnReturningData {
                    zonarosa_server_certificate_get_certificate($0, nativeHandle.const())
                }
            }
        }
    }

    public var signatureBytes: Data {
        return withNativeHandle { nativeHandle in
            failOnError {
                try invokeFnReturningData {
                    zonarosa_server_certificate_get_signature($0, nativeHandle.const())
                }
            }
        }
    }

    public var publicKey: PublicKey {
        return withNativeHandle { nativeHandle in
            failOnError {
                try invokeFnReturningNativeHandle {
                    zonarosa_server_certificate_get_key($0, nativeHandle.const())
                }
            }
        }
    }
}

extension ZonaRosaMutPointerServerCertificate: ZonaRosaMutPointer {
    public typealias ConstPointer = ZonaRosaConstPointerServerCertificate

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

extension ZonaRosaConstPointerServerCertificate: ZonaRosaConstPointer {
    public func toOpaque() -> OpaquePointer? {
        self.raw
    }
}

public class SenderCertificate: NativeHandleOwner<ZonaRosaMutPointerSenderCertificate>, @unchecked Sendable {
    public convenience init<Bytes: ContiguousBytes>(_ bytes: Bytes) throws {
        let handle = try bytes.withUnsafeBorrowedBuffer { bytes in
            try invokeFnReturningValueByPointer(.init()) {
                zonarosa_sender_certificate_deserialize($0, bytes)
            }
        }
        self.init(owned: NonNull(handle)!)
    }

    // For testing
    public convenience init(
        sender: SealedSenderAddress,
        publicKey: PublicKey,
        expiration: UInt64,
        signerCertificate: ServerCertificate,
        signerKey: PrivateKey
    ) throws {
        let result = try withAllBorrowed(publicKey, signerCertificate, signerKey) {
            publicKeyHandle,
            signerCertificateHandle,
            signerKeyHandle in
            try invokeFnReturningValueByPointer(.init()) {
                zonarosa_sender_certificate_new(
                    $0,
                    sender.uuidString,
                    sender.e164,
                    sender.deviceId,
                    publicKeyHandle.const(),
                    expiration,
                    signerCertificateHandle.const(),
                    signerKeyHandle.const()
                )
            }
        }
        self.init(owned: NonNull(result)!)
    }

    override internal class func destroyNativeHandle(
        _ handle: NonNull<ZonaRosaMutPointerSenderCertificate>
    ) -> ZonaRosaFfiErrorRef? {
        return zonarosa_sender_certificate_destroy(handle.pointer)
    }

    public var expiration: UInt64 {
        return withNativeHandle { nativeHandle in
            failOnError {
                try invokeFnReturningInteger {
                    zonarosa_sender_certificate_get_expiration($0, nativeHandle.const())
                }
            }
        }
    }

    public var deviceId: UInt32 {
        return withNativeHandle { nativeHandle in
            failOnError {
                try invokeFnReturningInteger {
                    zonarosa_sender_certificate_get_device_id($0, nativeHandle.const())
                }
            }
        }
    }

    public func serialize() -> Data {
        return withNativeHandle { nativeHandle in
            failOnError {
                try invokeFnReturningData {
                    zonarosa_sender_certificate_get_serialized($0, nativeHandle.const())
                }
            }
        }
    }

    public var certificateBytes: Data {
        return withNativeHandle { nativeHandle in
            failOnError {
                try invokeFnReturningData {
                    zonarosa_sender_certificate_get_certificate($0, nativeHandle.const())
                }
            }
        }
    }

    public var signatureBytes: Data {
        return withNativeHandle { nativeHandle in
            failOnError {
                try invokeFnReturningData {
                    zonarosa_sender_certificate_get_signature($0, nativeHandle.const())
                }
            }
        }
    }

    public var publicKey: PublicKey {
        return withNativeHandle { nativeHandle in
            failOnError {
                try invokeFnReturningNativeHandle {
                    zonarosa_sender_certificate_get_key($0, nativeHandle.const())
                }
            }
        }
    }

    public var senderUuid: String {
        return withNativeHandle { nativeHandle in
            failOnError {
                try invokeFnReturningString {
                    zonarosa_sender_certificate_get_sender_uuid($0, nativeHandle.const())
                }
            }
        }
    }

    /// Returns an ACI if the sender is a valid UUID, `nil` otherwise.
    ///
    /// In a future release SenderCertificate will *only* support ACIs.
    public var senderAci: Aci! {
        return try? Aci.parseFrom(serviceIdString: self.senderUuid)
    }

    public var senderE164: String? {
        return withNativeHandle { nativeHandle in
            failOnError {
                try invokeFnReturningOptionalString {
                    zonarosa_sender_certificate_get_sender_e164($0, nativeHandle.const())
                }
            }
        }
    }

    public var sender: SealedSenderAddress {
        return failOnError {
            try SealedSenderAddress(e164: self.senderE164, uuidString: self.senderUuid, deviceId: self.deviceId)
        }
    }

    public var serverCertificate: ServerCertificate {
        return withNativeHandle { nativeHandle in
            failOnError {
                try invokeFnReturningNativeHandle {
                    zonarosa_sender_certificate_get_server_certificate($0, nativeHandle.const())
                }
            }
        }
    }

    /// Validates `self` against the given trust root at the given current time.
    ///
    /// See ``validate(trustRoots:time:)`` for more information.
    public func validate(trustRoot: PublicKey, time: UInt64) -> Bool {
        return validate(trustRoots: [trustRoot], time: time)
    }

    /// Validates `self` against the given trust roots at the given current time.
    ///
    /// Checks the certificate against each key in `trustRoots` in constant time (that is, no result
    /// is produced until every key is checked), making sure **one** of them has signed its embedded
    /// server certificate. The `time` parameter is compared numerically against ``expiration``, and
    /// is not required to use any specific units, but ZonaRosa uses milliseconds since 1970.
    public func validate(trustRoots: [PublicKey], time: UInt64) -> Bool {
        // Use withExtendedLifetime instead of withNativeHandle for the arrays of wrapper objects,
        // which aren't compatible with withNativeHandle's simple lexical scoping.
        return withExtendedLifetime(trustRoots) {
            let trustRootHandles = trustRoots.map { $0.unsafeNativeHandle.const() }
            return
                (try? withAllBorrowed(self, .slice(trustRootHandles)) { certificateHandle, trustRootHandles in
                    try invokeFnReturningBool {
                        zonarosa_sender_certificate_validate($0, certificateHandle.const(), trustRootHandles, time)
                    }
                }) ?? false
        }
    }
}

extension ZonaRosaMutPointerSenderCertificate: ZonaRosaMutPointer {
    public typealias ConstPointer = ZonaRosaConstPointerSenderCertificate

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

extension ZonaRosaConstPointerSenderCertificate: ZonaRosaConstPointer {
    public func toOpaque() -> OpaquePointer? {
        self.raw
    }
}
