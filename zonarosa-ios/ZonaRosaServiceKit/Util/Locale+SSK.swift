//
// Copyright 2020 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation

public extension Locale {
    var isCJKV: Bool {
        guard let languageCode else { return false }
        return ["zk", "zh", "ja", "ko", "vi"].contains(languageCode)
    }
}
