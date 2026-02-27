//
// Copyright 2021 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import ZonaRosaServiceKit

public class AppContextUtils {

    private init() {}

    public static func openSystemSettingsAction(completion: (() -> Void)? = nil) -> ActionSheetAction? {
        guard CurrentAppContext().isMainApp else {
            return nil
        }

        return ActionSheetAction(title: CommonStrings.openSystemSettingsButton) { _ in
            CurrentAppContext().openSystemSettings()
            completion?()
        }
    }
}
