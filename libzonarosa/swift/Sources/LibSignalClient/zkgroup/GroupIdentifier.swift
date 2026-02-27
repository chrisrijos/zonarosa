//
// Copyright 2020-2021 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import ZonaRosaFfi

public class GroupIdentifier: HashableByteArray, CustomStringConvertible, @unchecked Sendable {
    public static let SIZE: Int = 32

    public required init(contents: Data) throws {
        try super.init(newContents: contents, expectedLength: GroupIdentifier.SIZE)
    }

    /// Returns the group ID as (lowercase) hexadecimal
    public var description: String {
        self.serialize().toHex()
    }
}

private func witnessGroupIdentifierIsHashable() throws {
    func isHashable<T: Hashable>(_: T) {}
    isHashable(try GroupIdentifier(contents: Data()))
}
