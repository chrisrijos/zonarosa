//
// Copyright 2024 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation

#if TESTABLE_BUILD

open class AttachmentViewOnceManagerMock: AttachmentViewOnceManager {

    public init() {}

    open func prepareViewOnceContentForDisplay(_ message: TSMessage) -> ViewOnceContent? {
        return nil
    }
}

#endif
