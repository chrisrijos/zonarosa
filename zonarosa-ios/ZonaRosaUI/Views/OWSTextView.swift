//
// Copyright 2022 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import ZonaRosaServiceKit

open class OWSTextView: UITextView {

    override init(frame: CGRect, textContainer: NSTextContainer?) {
        super.init(frame: frame, textContainer: textContainer)
        self.disableAiWritingTools()
        keyboardAppearance = Theme.keyboardAppearance
        dataDetectorTypes = []
    }

    public required init?(coder: NSCoder) {
        owsFail("Not implemented!")
    }
}
