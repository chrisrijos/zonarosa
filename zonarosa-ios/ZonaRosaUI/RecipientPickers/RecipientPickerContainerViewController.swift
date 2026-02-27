//
// Copyright 2022 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation

open class RecipientPickerContainerViewController: OWSViewController, OWSNavigationChildController {
    public let recipientPicker = RecipientPickerViewController()

    public var childForOWSNavigationConfiguration: OWSNavigationChildController? {
        return recipientPicker
    }

    public func addRecipientPicker() {
        addChild(recipientPicker)
        view.addSubview(recipientPicker.view)
        recipientPicker.view.autoPinEdgesToSuperviewEdges()
        recipientPicker.didMove(toParent: self)
    }
}
