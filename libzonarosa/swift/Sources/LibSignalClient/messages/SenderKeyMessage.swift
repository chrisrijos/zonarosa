//
// Copyright 2020-2022 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import ZonaRosaFfi

public class SenderKeyMessage: NativeHandleOwner<ZonaRosaMutPointerSenderKeyMessage> {
    override internal class func destroyNativeHandle(
        _ handle: NonNull<ZonaRosaMutPointerSenderKeyMessage>
    ) -> ZonaRosaFfiErrorRef? {
        return zonarosa_sender_key_message_destroy(handle.pointer)
    }

    public convenience init<Bytes: ContiguousBytes>(bytes: Bytes) throws {
        let result = try bytes.withUnsafeBorrowedBuffer { bytes in
            try invokeFnReturningValueByPointer(.init()) {
                zonarosa_sender_key_message_deserialize($0, bytes)
            }
        }
        self.init(owned: NonNull(result)!)
    }

    public var distributionId: UUID {
        return withNativeHandle { nativeHandle in
            failOnError {
                try invokeFnReturningUuid {
                    zonarosa_sender_key_message_get_distribution_id($0, nativeHandle.const())
                }
            }
        }
    }

    public var chainId: UInt32 {
        return withNativeHandle { nativeHandle in
            failOnError {
                try invokeFnReturningInteger {
                    zonarosa_sender_key_message_get_chain_id($0, nativeHandle.const())
                }
            }
        }
    }

    public var iteration: UInt32 {
        return withNativeHandle { nativeHandle in
            failOnError {
                try invokeFnReturningInteger {
                    zonarosa_sender_key_message_get_iteration($0, nativeHandle.const())
                }
            }
        }
    }

    public func serialize() -> Data {
        return withNativeHandle { nativeHandle in
            failOnError {
                try invokeFnReturningData {
                    zonarosa_sender_key_message_serialize($0, nativeHandle.const())
                }
            }
        }
    }

    public var ciphertext: Data {
        return withNativeHandle { nativeHandle in
            failOnError {
                try invokeFnReturningData {
                    zonarosa_sender_key_message_get_cipher_text($0, nativeHandle.const())
                }
            }
        }
    }

    public func verifySignature(against key: PublicKey) throws -> Bool {
        return try withAllBorrowed(self, key) { messageHandle, keyHandle in
            try invokeFnReturningBool {
                zonarosa_sender_key_message_verify_signature($0, messageHandle.const(), keyHandle.const())
            }
        }
    }
}

extension ZonaRosaMutPointerSenderKeyMessage: ZonaRosaMutPointer {
    public typealias ConstPointer = ZonaRosaConstPointerSenderKeyMessage

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

extension ZonaRosaConstPointerSenderKeyMessage: ZonaRosaConstPointer {
    public func toOpaque() -> OpaquePointer? {
        self.raw
    }
}
