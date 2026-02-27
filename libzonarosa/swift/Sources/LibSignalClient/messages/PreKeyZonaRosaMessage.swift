//
// Copyright 2020-2022 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import ZonaRosaFfi

public class PreKeyZonaRosaMessage: NativeHandleOwner<ZonaRosaMutPointerPreKeyZonaRosaMessage> {
    override internal class func destroyNativeHandle(
        _ handle: NonNull<ZonaRosaMutPointerPreKeyZonaRosaMessage>
    ) -> ZonaRosaFfiErrorRef? {
        return zonarosa_pre_key_zonarosa_message_destroy(handle.pointer)
    }

    public convenience init<Bytes: ContiguousBytes>(bytes: Bytes) throws {
        let result = try bytes.withUnsafeBorrowedBuffer { bytes in
            try invokeFnReturningValueByPointer(.init()) {
                zonarosa_pre_key_zonarosa_message_deserialize($0, bytes)
            }
        }
        self.init(owned: NonNull(result)!)
    }

    public func serialize() throws -> Data {
        return try withNativeHandle { nativeHandle in
            try invokeFnReturningData {
                zonarosa_pre_key_zonarosa_message_serialize($0, nativeHandle.const())
            }
        }
    }

    public func version() throws -> UInt32 {
        return try withNativeHandle { nativeHandle in
            try invokeFnReturningInteger {
                zonarosa_pre_key_zonarosa_message_get_version($0, nativeHandle.const())
            }
        }
    }

    public func registrationId() throws -> UInt32 {
        return try withNativeHandle { nativeHandle in
            try invokeFnReturningInteger {
                zonarosa_pre_key_zonarosa_message_get_registration_id($0, nativeHandle.const())
            }
        }
    }

    public func preKeyId() throws -> UInt32? {
        let id = try withNativeHandle { nativeHandle in
            try invokeFnReturningInteger {
                zonarosa_pre_key_zonarosa_message_get_pre_key_id($0, nativeHandle.const())
            }
        }

        if id == 0xFFFF_FFFF {
            return nil
        } else {
            return id
        }
    }

    public var signedPreKeyId: UInt32 {
        return withNativeHandle { nativeHandle in
            failOnError {
                try invokeFnReturningInteger {
                    zonarosa_pre_key_zonarosa_message_get_signed_pre_key_id($0, nativeHandle.const())
                }
            }
        }
    }

    public var baseKey: PublicKey {
        return withNativeHandle { nativeHandle in
            failOnError {
                try invokeFnReturningNativeHandle {
                    zonarosa_pre_key_zonarosa_message_get_base_key($0, nativeHandle.const())
                }
            }
        }
    }

    public var identityKey: PublicKey {
        return withNativeHandle { nativeHandle in
            failOnError {
                try invokeFnReturningNativeHandle {
                    zonarosa_pre_key_zonarosa_message_get_identity_key($0, nativeHandle.const())
                }
            }
        }
    }

    public var zonarosaMessage: ZonaRosaMessage {
        return withNativeHandle { nativeHandle in
            failOnError {
                try invokeFnReturningNativeHandle {
                    zonarosa_pre_key_zonarosa_message_get_zonarosa_message($0, nativeHandle.const())
                }
            }
        }
    }
}

extension ZonaRosaMutPointerPreKeyZonaRosaMessage: ZonaRosaMutPointer {
    public typealias ConstPointer = ZonaRosaConstPointerPreKeyZonaRosaMessage

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

extension ZonaRosaConstPointerPreKeyZonaRosaMessage: ZonaRosaConstPointer {
    public func toOpaque() -> OpaquePointer? {
        self.raw
    }
}
