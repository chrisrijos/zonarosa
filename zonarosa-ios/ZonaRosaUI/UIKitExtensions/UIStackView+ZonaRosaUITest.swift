//
// Copyright 2023 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import ZonaRosaUI
import UIKit
import XCTest

final class UIStackViewZonaRosaUITest: XCTestCase {
    func testRemoveArrangedSubviewsAfter() {
        let a = UIView()
        let b = UIView()
        let c = UIView()
        let d = UIView()

        let stack = UIStackView(arrangedSubviews: [a, b, c])

        stack.removeArrangedSubviewsAfter(d)
        XCTAssertEqual(stack.arrangedSubviews, [a, b, c])

        stack.removeArrangedSubviewsAfter(c)
        XCTAssertEqual(stack.arrangedSubviews, [a, b, c])

        stack.removeArrangedSubviewsAfter(b)
        XCTAssertEqual(stack.arrangedSubviews, [a, b])

        stack.removeArrangedSubviewsAfter(a)
        XCTAssertEqual(stack.arrangedSubviews, [a])
    }
}
