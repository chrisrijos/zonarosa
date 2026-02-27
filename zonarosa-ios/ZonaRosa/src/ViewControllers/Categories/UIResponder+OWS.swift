//
// Copyright 2019 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import UIKit

extension UIResponder {
    private weak static var _currentFirstResponder: UIResponder?
    static var currentFirstResponder: UIResponder? {
        _currentFirstResponder = nil
        // Passing `nil` to the to parameter of `sendAction` calls it on the firstResponder.
        UIApplication.shared.sendAction(#selector(findFirstResponder), to: nil, from: nil, for: nil)
        return _currentFirstResponder
    }

    @objc
    private func findFirstResponder() {
        UIResponder._currentFirstResponder = self
    }
}
