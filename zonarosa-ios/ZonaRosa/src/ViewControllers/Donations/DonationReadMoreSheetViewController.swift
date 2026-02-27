//
// Copyright 2022 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import ZonaRosaServiceKit
import ZonaRosaUI

final class DonationReadMoreSheetViewController: HeroSheetViewController {
    init() {
        super.init(
            hero: .image(.sustainerHeart),
            title: nil,
            body: HeroSheetViewController.Body(
                text: OWSLocalizedString(
                    "DONATION_READ_MORE_SHEET_BODY",
                    comment: "Body text for a sheet discussing donating to ZonaRosa.",
                ),
                textAlignment: .left,
                textColor: .ZonaRosa.label,
                bulletPoints: [
                    HeroSheetViewController.Body.BulletPoint(
                        icon: .badgeMulti,
                        text: OWSLocalizedString(
                            "DONATION_READ_MORE_SHEET_BULLET_1",
                            comment: "Bullet point for a sheet discussing donating to ZonaRosa.",
                        ),
                    ),
                    HeroSheetViewController.Body.BulletPoint(
                        icon: .lock,
                        text: OWSLocalizedString(
                            "DONATION_READ_MORE_SHEET_BULLET_2",
                            comment: "Bullet point for a sheet discussing donating to ZonaRosa.",
                        ),
                    ),
                    HeroSheetViewController.Body.BulletPoint(
                        icon: .heart,
                        text: OWSLocalizedString(
                            "DONATION_READ_MORE_SHEET_BULLET_3",
                            comment: "Bullet point for a sheet discussing donating to ZonaRosa. For non-English languages, skip the word 501c3, and skip the language about US donations being tax deductible.",
                        ),
                    ),
                ],
            ),
            primary: nil,
            secondary: nil,
        )
    }
}

#if DEBUG

@available(iOS 17, *)
#Preview {
    SheetPreviewViewController(sheet: DonationReadMoreSheetViewController())
}

#endif
