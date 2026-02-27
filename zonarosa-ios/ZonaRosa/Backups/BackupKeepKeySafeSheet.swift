//
// Copyright 2025 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import ZonaRosaServiceKit
import ZonaRosaUI

class BackupKeepKeySafeSheet: HeroSheetViewController {
    /// - Parameter onContinue
    /// Called after dismissing this sheet when the user taps "Continue",
    /// indicating acknowledgement of the "keep key safe" warning.
    /// - Parameter secondaryButton
    /// Used as this sheet's secondary button.
    init(
        onContinue: @escaping () -> Void,
        secondaryButton: Button,
    ) {
        super.init(
            hero: .image(.backupsKey),
            title: OWSLocalizedString(
                "BACKUP_ONBOARDING_CONFIRM_KEY_KEEP_KEY_SAFE_SHEET_TITLE",
                comment: "Title for a sheet warning users to their 'Recovery Key' safe.",
            ),
            body: OWSLocalizedString(
                "BACKUP_ONBOARDING_CONFIRM_KEY_KEEP_KEY_SAFE_SHEET_BODY",
                comment: "Body for a sheet warning users to their 'Recovery Key' safe.",
            ),
            primaryButton: Button(
                title: CommonStrings.continueButton,
                action: { sheet in
                    sheet.dismiss(animated: true) {
                        onContinue()
                    }
                },
            ),
            secondaryButton: secondaryButton,
        )
    }
}
