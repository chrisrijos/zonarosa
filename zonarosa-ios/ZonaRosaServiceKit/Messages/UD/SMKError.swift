//
// Copyright 2022 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation

public enum SMKError: Error {
    case assertionError(description: String)
    case invalidInput(_ description: String)
}
