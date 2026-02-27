//
// Copyright 2020-2022 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import ZonaRosaFfi

public class ServerPublicParams: NativeHandleOwner<ZonaRosaMutPointerServerPublicParams> {
    public convenience init(contents: Data) throws {
        let handle = try contents.withUnsafeBorrowedBuffer { contents in
            try invokeFnReturningValueByPointer(.init()) {
                zonarosa_server_public_params_deserialize($0, contents)
            }
        }
        self.init(owned: NonNull(handle)!)
    }

    required init(owned: NonNull<ZonaRosaMutPointerServerPublicParams>) {
        super.init(owned: owned)
    }

    /**
     * Get the serialized form of the params' endorsement key.
     *
     * Allows decoupling RingRTC's use of endorsements from libzonarosa's.
     */
    public var endorsementPublicKey: Data {
        return failOnError {
            try self.withNativeHandle { handle in
                try invokeFnReturningData {
                    zonarosa_server_public_params_get_endorsement_public_key($0, handle.const())
                }
            }
        }
    }

    public func verifySignature(message: Data, notarySignature: NotarySignature) throws {
        try withNativeHandle { contents in
            try message.withUnsafeBorrowedBuffer { message in
                try notarySignature.withUnsafePointerToSerialized { notarySignature in
                    try checkError(
                        zonarosa_server_public_params_verify_signature(contents.const(), message, notarySignature)
                    )
                }
            }
        }
    }

    public func serialize() -> Data {
        return failOnError {
            try withNativeHandle { handle in
                try invokeFnReturningData {
                    zonarosa_server_public_params_serialize($0, handle.const())
                }
            }
        }
    }

    override internal class func destroyNativeHandle(
        _ handle: NonNull<ZonaRosaMutPointerServerPublicParams>
    ) -> ZonaRosaFfiErrorRef? {
        zonarosa_server_public_params_destroy(handle.pointer)
    }
}

extension ZonaRosaMutPointerServerPublicParams: ZonaRosaMutPointer {
    public typealias ConstPointer = ZonaRosaConstPointerServerPublicParams

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

extension ZonaRosaConstPointerServerPublicParams: ZonaRosaConstPointer {
    public func toOpaque() -> OpaquePointer? {
        self.raw
    }
}
