//
// Copyright 2020-2022 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import ZonaRosaFfi

public class ServerSecretParams: NativeHandleOwner<ZonaRosaMutPointerServerSecretParams> {
    public static func generate() throws -> ServerSecretParams {
        return try self.generate(randomness: Randomness.generate())
    }

    public static func generate(randomness: Randomness) throws -> ServerSecretParams {
        return try randomness.withUnsafePointerToBytes { randomness in
            try invokeFnReturningNativeHandle {
                zonarosa_server_secret_params_generate_deterministic($0, randomness)
            }
        }
    }

    public convenience init(contents: Data) throws {
        let handle = try contents.withUnsafeBorrowedBuffer { contents in
            try invokeFnReturningValueByPointer(.init()) {
                zonarosa_server_secret_params_deserialize($0, contents)
            }
        }
        self.init(owned: NonNull(handle)!)
    }

    required init(owned: NonNull<ZonaRosaMutPointerServerSecretParams>) {
        super.init(owned: owned)
    }

    public func serialize() -> Data {
        return failOnError {
            try withNativeHandle { handle in
                try invokeFnReturningData {
                    zonarosa_server_secret_params_serialize($0, handle.const())
                }
            }
        }
    }

    public func getPublicParams() throws -> ServerPublicParams {
        return try withNativeHandle { contents in
            try invokeFnReturningNativeHandle {
                zonarosa_server_secret_params_get_public_params($0, contents.const())
            }
        }
    }

    public func sign(message: Data) throws -> NotarySignature {
        return try self.sign(randomness: Randomness.generate(), message: message)
    }

    public func sign(randomness: Randomness, message: Data) throws -> NotarySignature {
        return try withNativeHandle { contents in
            try randomness.withUnsafePointerToBytes { randomness in
                try message.withUnsafeBorrowedBuffer { message in
                    try invokeFnReturningSerialized {
                        zonarosa_server_secret_params_sign_deterministic($0, contents.const(), randomness, message)
                    }
                }
            }
        }
    }

    override internal class func destroyNativeHandle(
        _ handle: NonNull<ZonaRosaMutPointerServerSecretParams>
    ) -> ZonaRosaFfiErrorRef? {
        zonarosa_server_secret_params_destroy(handle.pointer)
    }
}

extension ZonaRosaMutPointerServerSecretParams: ZonaRosaMutPointer {
    public typealias ConstPointer = ZonaRosaConstPointerServerSecretParams

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

extension ZonaRosaConstPointerServerSecretParams: ZonaRosaConstPointer {
    public func toOpaque() -> OpaquePointer? {
        self.raw
    }
}
