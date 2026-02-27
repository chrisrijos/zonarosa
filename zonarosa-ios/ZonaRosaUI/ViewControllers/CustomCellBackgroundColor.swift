//
// Copyright 2023 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import UIKit

public protocol CustomBackgroundColorCell: UITableViewCell {
    func customBackgroundColor(forceDarkMode: Bool) -> UIColor
    func customSelectedBackgroundColor(forceDarkMode: Bool) -> UIColor
}
