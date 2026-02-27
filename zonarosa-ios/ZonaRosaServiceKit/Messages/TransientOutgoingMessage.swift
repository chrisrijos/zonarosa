//
// Copyright 2026 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation

@objc(TransientOutgoingMessage)
public class TransientOutgoingMessage: TSOutgoingMessage {
    override public class var supportsSecureCoding: Bool { true }

    override public var shouldBeSaved: Bool { false }
}
