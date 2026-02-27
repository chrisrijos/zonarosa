//
// Copyright 2021-2022 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import ZonaRosaFfi

public class PlaintextContent: NativeHandleOwner<ZonaRosaMutPointerPlaintextContent> {
    override internal class func destroyNativeHandle(
        _ handle: NonNull<ZonaRosaMutPointerPlaintextContent>
    ) -> ZonaRosaFfiErrorRef? {
        return zonarosa_plaintext_content_destroy(handle.pointer)
    }

    public convenience init<Bytes: ContiguousBytes>(bytes: Bytes) throws {
        let result = try bytes.withUnsafeBorrowedBuffer { bytes in
            try invokeFnReturningValueByPointer(.init()) {
                zonarosa_plaintext_content_deserialize($0, bytes)
            }
        }
        self.init(owned: NonNull(result)!)
    }

    public convenience init(_ decryptionError: DecryptionErrorMessage) {
        let result = decryptionError.withNativeHandle { decryptionErrorHandle in
            failOnError {
                try invokeFnReturningValueByPointer(.init()) {
                    zonarosa_plaintext_content_from_decryption_error_message($0, decryptionErrorHandle.const())
                }
            }
        }
        self.init(owned: NonNull(result)!)
    }

    public func serialize() -> Data {
        return withNativeHandle { nativeHandle in
            failOnError {
                try invokeFnReturningData {
                    zonarosa_plaintext_content_serialize($0, nativeHandle.const())
                }
            }
        }
    }

    public var body: Data {
        return withNativeHandle { nativeHandle in
            failOnError {
                try invokeFnReturningData {
                    zonarosa_plaintext_content_get_body($0, nativeHandle.const())
                }
            }
        }
    }
}

extension ZonaRosaMutPointerPlaintextContent: ZonaRosaMutPointer {
    public typealias ConstPointer = ZonaRosaConstPointerPlaintextContent

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

extension ZonaRosaConstPointerPlaintextContent: ZonaRosaConstPointer {
    public func toOpaque() -> OpaquePointer? {
        self.raw
    }
}

public class DecryptionErrorMessage: NativeHandleOwner<ZonaRosaMutPointerDecryptionErrorMessage> {
    override internal class func destroyNativeHandle(
        _ handle: NonNull<ZonaRosaMutPointerDecryptionErrorMessage>
    ) -> ZonaRosaFfiErrorRef? {
        return zonarosa_decryption_error_message_destroy(handle.pointer)
    }

    public convenience init<Bytes: ContiguousBytes>(bytes: Bytes) throws {
        let result = try bytes.withUnsafeBorrowedBuffer { bytes in
            try invokeFnReturningValueByPointer(.init()) {
                zonarosa_decryption_error_message_deserialize($0, bytes)
            }
        }
        self.init(owned: NonNull(result)!)
    }

    public convenience init<Bytes: ContiguousBytes>(
        originalMessageBytes bytes: Bytes,
        type: CiphertextMessage.MessageType,
        timestamp: UInt64,
        originalSenderDeviceId: UInt32
    ) throws {
        let result = try bytes.withUnsafeBorrowedBuffer { bytes in
            try invokeFnReturningValueByPointer(.init()) {
                zonarosa_decryption_error_message_for_original_message(
                    $0,
                    bytes,
                    type.rawValue,
                    timestamp,
                    originalSenderDeviceId
                )
            }
        }
        self.init(owned: NonNull(result)!)
    }

    // For testing
    public static func extractFromSerializedContent<Bytes: ContiguousBytes>(
        _ bytes: Bytes
    ) throws -> DecryptionErrorMessage {
        return try bytes.withUnsafeBorrowedBuffer { buffer in
            try invokeFnReturningNativeHandle {
                zonarosa_decryption_error_message_extract_from_serialized_content($0, buffer)
            }
        }
    }

    public func serialize() -> Data {
        return withNativeHandle { nativeHandle in
            failOnError {
                try invokeFnReturningData {
                    zonarosa_decryption_error_message_serialize($0, nativeHandle.const())
                }
            }
        }
    }

    public var ratchetKey: PublicKey? {
        return withNativeHandle { nativeHandle in
            failOnError {
                try invokeFnReturningOptionalNativeHandle {
                    zonarosa_decryption_error_message_get_ratchet_key($0, nativeHandle.const())
                }
            }
        }
    }

    public var timestamp: UInt64 {
        return withNativeHandle { nativeHandle in
            failOnError {
                try invokeFnReturningInteger {
                    zonarosa_decryption_error_message_get_timestamp($0, nativeHandle.const())
                }
            }
        }
    }

    public var deviceId: UInt32 {
        return withNativeHandle { nativeHandle in
            failOnError {
                try invokeFnReturningInteger {
                    zonarosa_decryption_error_message_get_device_id($0, nativeHandle.const())
                }
            }
        }
    }
}

extension ZonaRosaMutPointerDecryptionErrorMessage: ZonaRosaMutPointer {
    public typealias ConstPointer = ZonaRosaConstPointerDecryptionErrorMessage

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

extension ZonaRosaConstPointerDecryptionErrorMessage: ZonaRosaConstPointer {
    public func toOpaque() -> OpaquePointer? {
        self.raw
    }
}
