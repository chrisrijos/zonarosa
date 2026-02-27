//
// Copyright 2021-2022 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import ZonaRosaFfi

public enum KeyFormat: UInt8, CaseIterable, Sendable {
    // PKCS#8 is the default for backward compatibility
    case pkcs8 = 0
    case keySpecific = 1
}

public struct DeviceTransferKey: Sendable {
    public let privateKey: Data

    private init(privateKey: Data) {
        self.privateKey = privateKey
    }

    public static func generate(formattedAs keyFormat: KeyFormat = .pkcs8) -> Self {
        let privateKey = failOnError {
            try invokeFnReturningData {
                zonarosa_device_transfer_generate_private_key_with_format($0, keyFormat.rawValue)
            }
        }

        return Self(privateKey: privateKey)
    }

    public func privateKeyMaterial() -> Data {
        return self.privateKey
    }

    public func generateCertificate(_ name: String, _ daysTilExpire: Int) -> Data {
        return self.privateKey.withUnsafeBorrowedBuffer { privateKeyBuffer in
            failOnError {
                try invokeFnReturningData {
                    zonarosa_device_transfer_generate_certificate(
                        $0,
                        privateKeyBuffer,
                        name,
                        UInt32(daysTilExpire)
                    )
                }
            }
        }
    }
}
