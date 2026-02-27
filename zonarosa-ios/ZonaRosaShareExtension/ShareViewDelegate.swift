//
// Copyright 2017 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
public import ZonaRosaUI

// All Observer methods will be invoked from the main thread.
public protocol ShareViewDelegate: AnyObject {
    func shareViewWillSend()
    func shareViewWasCompleted()
    func shareViewWasCancelled()
    func shareViewFailed(error: Error)
    var shareViewNavigationController: OWSNavigationController { get }
}
