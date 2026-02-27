//
// Copyright 2024 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation

public protocol Streamable {

    func remove(from: RunLoop, forMode: RunLoop.Mode)

    func schedule(in: RunLoop, forMode: RunLoop.Mode)

    func close() throws
}
