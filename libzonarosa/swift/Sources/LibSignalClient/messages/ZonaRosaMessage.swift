//
// Copyright 2020-2022 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import ZonaRosaFfi

public class ZonaRosaMessage: NativeHandleOwner<ZonaRosaMutPointerZonaRosaMessage> {
    override internal class func destroyNativeHandle(
        _ handle: NonNull<ZonaRosaMutPointerZonaRosaMessage>
    ) -> ZonaRosaFfiErrorRef? {
        return zonarosa_message_destroy(handle.pointer)
    }

    public convenience init<Bytes: ContiguousBytes>(bytes: Bytes) throws {
        let result = try bytes.withUnsafeBorrowedBuffer { bytes in
            try invokeFnReturningValueByPointer(.init()) {
                zonarosa_message_deserialize($0, bytes)
            }
        }
        self.init(owned: NonNull(result)!)
    }

    public var senderRatchetKey: PublicKey {
        return withNativeHandle { nativeHandle in
            failOnError {
                try invokeFnReturningNativeHandle {
                    zonarosa_message_get_sender_ratchet_key($0, nativeHandle.const())
                }
            }
        }
    }

    public var body: Data {
        return withNativeHandle { nativeHandle in
            failOnError {
                try invokeFnReturningData {
                    zonarosa_message_get_body($0, nativeHandle.const())
                }
            }
        }
    }

    public func serialize() -> Data {
        return withNativeHandle { nativeHandle in
            failOnError {
                try invokeFnReturningData {
                    zonarosa_message_get_serialized($0, nativeHandle.const())
                }
            }
        }
    }

    public var messageVersion: UInt32 {
        return withNativeHandle { nativeHandle in
            failOnError {
                try invokeFnReturningInteger {
                    zonarosa_message_get_message_version($0, nativeHandle.const())
                }
            }
        }
    }

    public var counter: UInt32 {
        return withNativeHandle { nativeHandle in
            failOnError {
                try invokeFnReturningInteger {
                    zonarosa_message_get_counter($0, nativeHandle.const())
                }
            }
        }
    }

    public func verifyMac<Bytes: ContiguousBytes>(
        sender: PublicKey,
        receiver: PublicKey,
        macKey: Bytes
    ) throws -> Bool {
        return try withAllBorrowed(
            self,
            sender,
            receiver,
            .bytes(macKey)
        ) { messageHandle, senderHandle, receiverHandle, macKey in
            try invokeFnReturningBool {
                zonarosa_message_verify_mac(
                    $0,
                    messageHandle.const(),
                    senderHandle.const(),
                    receiverHandle.const(),
                    macKey
                )
            }
        }
    }
}

extension ZonaRosaMutPointerZonaRosaMessage: ZonaRosaMutPointer {
    public typealias ConstPointer = ZonaRosaConstPointerZonaRosaMessage

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

extension ZonaRosaConstPointerZonaRosaMessage: ZonaRosaConstPointer {
    public func toOpaque() -> OpaquePointer? {
        self.raw
    }
}
