//
// Copyright 2019 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import UIKit

class OWSImagePickerController: UIImagePickerController {

    // MARK: Orientation

    override var supportedInterfaceOrientations: UIInterfaceOrientationMask {
        return UIDevice.current.defaultSupportedOrientations
    }
}
