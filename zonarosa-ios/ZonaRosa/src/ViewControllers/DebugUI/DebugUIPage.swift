//
// Copyright 2023 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import ZonaRosaServiceKit
import ZonaRosaUI

#if USE_DEBUG_UI

protocol DebugUIPage {

    var name: String { get }

    func section(thread: TSThread?) -> OWSTableSection?
}

#endif
