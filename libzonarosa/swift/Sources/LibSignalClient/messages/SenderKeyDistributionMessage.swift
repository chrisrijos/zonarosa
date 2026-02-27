//
// Copyright 2020-2022 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import ZonaRosaFfi

public class SenderKeyDistributionMessage: NativeHandleOwner<ZonaRosaMutPointerSenderKeyDistributionMessage> {
    override internal class func destroyNativeHandle(
        _ handle: NonNull<ZonaRosaMutPointerSenderKeyDistributionMessage>
    ) -> ZonaRosaFfiErrorRef? {
        return zonarosa_sender_key_distribution_message_destroy(handle.pointer)
    }

    public convenience init(
        from sender: ProtocolAddress,
        distributionId: UUID,
        store: SenderKeyStore,
        context: StoreContext
    ) throws {
        let result = try sender.withNativeHandle { senderHandle in
            try withSenderKeyStore(store, context) { store in
                try invokeFnReturningValueByPointer(.init()) {
                    zonarosa_sender_key_distribution_message_create(
                        $0,
                        senderHandle.const(),
                        ZonaRosaUuid(bytes: distributionId.uuid),
                        store
                    )
                }
            }
        }
        self.init(owned: NonNull(result)!)
    }

    public convenience init(bytes: Data) throws {
        let result = try bytes.withUnsafeBorrowedBuffer { bytes in
            try invokeFnReturningValueByPointer(.init()) {
                zonarosa_sender_key_distribution_message_deserialize($0, bytes)
            }
        }
        self.init(owned: NonNull(result)!)
    }

    public var signatureKey: PublicKey {
        return withNativeHandle { nativeHandle in
            failOnError {
                try invokeFnReturningNativeHandle {
                    zonarosa_sender_key_distribution_message_get_signature_key($0, nativeHandle.const())
                }
            }
        }
    }

    public var distributionId: UUID {
        return withNativeHandle { nativeHandle in
            failOnError {
                try invokeFnReturningUuid {
                    zonarosa_sender_key_distribution_message_get_distribution_id($0, nativeHandle.const())
                }
            }
        }
    }

    public var chainId: UInt32 {
        return withNativeHandle { nativeHandle in
            failOnError {
                try invokeFnReturningInteger {
                    zonarosa_sender_key_distribution_message_get_chain_id($0, nativeHandle.const())
                }
            }
        }
    }

    public var iteration: UInt32 {
        return withNativeHandle { nativeHandle in
            failOnError {
                try invokeFnReturningInteger {
                    zonarosa_sender_key_distribution_message_get_iteration($0, nativeHandle.const())
                }
            }
        }
    }

    public func serialize() -> Data {
        return withNativeHandle { nativeHandle in
            failOnError {
                try invokeFnReturningData {
                    zonarosa_sender_key_distribution_message_serialize($0, nativeHandle.const())
                }
            }
        }
    }

    public var chainKey: Data {
        return withNativeHandle { nativeHandle in
            failOnError {
                try invokeFnReturningData {
                    zonarosa_sender_key_distribution_message_get_chain_key($0, nativeHandle.const())
                }
            }
        }
    }
}

extension ZonaRosaMutPointerSenderKeyDistributionMessage: ZonaRosaMutPointer {
    public typealias ConstPointer = ZonaRosaConstPointerSenderKeyDistributionMessage

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

extension ZonaRosaConstPointerSenderKeyDistributionMessage: ZonaRosaConstPointer {
    public func toOpaque() -> OpaquePointer? {
        self.raw
    }
}
