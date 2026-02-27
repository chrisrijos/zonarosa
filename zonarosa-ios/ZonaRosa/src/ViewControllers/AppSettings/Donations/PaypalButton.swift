//
// Copyright 2022 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import ZonaRosaServiceKit
import ZonaRosaUI
import UIKit

class PaypalButton: UIButton {
    private let actionBlock: () -> Void

    init(actionBlock: @escaping () -> Void) {
        self.actionBlock = actionBlock

        super.init(frame: .zero)

        addTarget(self, action: #selector(didTouchUpInside), for: .touchUpInside)

        configureStyling()
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) is not implemented.")
    }

    // MARK: Styling

    private func configureStyling() {
        setImage(UIImage(named: "paypal-logo"), for: .normal)
        ows_adjustsImageWhenDisabled = false
        ows_adjustsImageWhenHighlighted = false
        if #available(iOS 26.0, *) {
            configuration = .prominentGlass()
            tintColor = UIColor(rgbHex: 0xF6C757)
        } else {
            layer.cornerRadius = 12
            backgroundColor = UIColor(rgbHex: 0xF6C757)
        }
    }

    // MARK: Actions

    @objc
    private func didTouchUpInside() {
        actionBlock()
    }
}
