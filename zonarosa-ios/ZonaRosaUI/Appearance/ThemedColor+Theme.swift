//
// Copyright 2023 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
public import ZonaRosaServiceKit

extension ThemedColor {

    public var forCurrentTheme: UIColor {
        return self.color(isDarkThemeEnabled: Theme.isDarkThemeEnabled)
    }

    public static func fixed(_ color: UIColor) -> ThemedColor {
        return ThemedColor(light: color, dark: color)
    }
}
