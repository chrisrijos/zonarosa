//
// Copyright 2021 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import ZonaRosaFfi

public struct Randomness: Sendable {
    public var bytes: ZonaRosaRandomnessBytes

    public init(_ bytes: ZonaRosaRandomnessBytes) {
        self.bytes = bytes
    }

    static func generate() throws -> Randomness {
        var bytes: ZonaRosaRandomnessBytes = (
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        )
        try withUnsafeMutableBytes(of: &bytes) {
            try fillRandom($0)
        }
        return Randomness(bytes)
    }

    func withUnsafePointerToBytes<Result>(
        _ callback: (UnsafePointer<ZonaRosaRandomnessBytes>) throws -> Result
    ) rethrows -> Result {
        try withUnsafePointer(to: self.bytes, callback)
    }
}
