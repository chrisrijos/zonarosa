//
// Copyright 2019 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation

@objc
public enum StickerError: Int, Error {
    case invalidInput
    case noSticker
    case corruptData
}
