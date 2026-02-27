//
// Copyright 2020-2021 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import ZonaRosaFfi

public class CiphertextMessage: NativeHandleOwner<ZonaRosaMutPointerCiphertextMessage> {
    public struct MessageType: RawRepresentable, Hashable, Sendable {
        public var rawValue: UInt8
        public init(rawValue: UInt8) {
            self.rawValue = rawValue
        }

        internal init(_ knownType: ZonaRosaCiphertextMessageType) {
            self.init(rawValue: UInt8(knownType.rawValue))
        }

        public static var zonarosa: Self {
            return Self(ZonaRosaCiphertextMessageTypeWhisper)
        }

        public static var preKey: Self {
            return Self(ZonaRosaCiphertextMessageTypePreKey)
        }

        public static var senderKey: Self {
            return Self(ZonaRosaCiphertextMessageTypeSenderKey)
        }

        public static var plaintext: Self {
            return Self(ZonaRosaCiphertextMessageTypePlaintext)
        }
    }

    override internal class func destroyNativeHandle(
        _ handle: NonNull<ZonaRosaMutPointerCiphertextMessage>
    ) -> ZonaRosaFfiErrorRef? {
        return zonarosa_ciphertext_message_destroy(handle.pointer)
    }

    public convenience init(_ plaintextContent: PlaintextContent) {
        let result = plaintextContent.withNativeHandle { plaintextContentHandle in
            failOnError {
                try invokeFnReturningValueByPointer(.init()) {
                    zonarosa_ciphertext_message_from_plaintext_content($0, plaintextContentHandle.const())
                }
            }
        }
        self.init(owned: NonNull(result)!)
    }

    public func serialize() -> Data {
        return withNativeHandle { nativeHandle in
            failOnError {
                try invokeFnReturningData {
                    zonarosa_ciphertext_message_serialize($0, nativeHandle.const())
                }
            }
        }
    }

    public var messageType: MessageType {
        let rawValue = withNativeHandle { nativeHandle in
            failOnError {
                try invokeFnReturningInteger {
                    zonarosa_ciphertext_message_type($0, nativeHandle.const())
                }
            }
        }
        return MessageType(rawValue: rawValue)
    }
}

extension ZonaRosaMutPointerCiphertextMessage: ZonaRosaMutPointer {
    public typealias ConstPointer = ZonaRosaConstPointerCiphertextMessage

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

extension ZonaRosaConstPointerCiphertextMessage: ZonaRosaConstPointer {
    public func toOpaque() -> OpaquePointer? {
        self.raw
    }
}
