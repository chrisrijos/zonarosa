//
// Copyright 2023 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import ZonaRosaServiceKit
import ZonaRosaUI

class ProvisioningBaseViewController: OWSViewController, OWSNavigationChildController {

    // Unlike a delegate, we can and should retain a strong reference to the ProvisioningController.
    let provisioningController: ProvisioningController

    init(provisioningController: ProvisioningController) {
        self.provisioningController = provisioningController
        super.init()
    }

    // MARK: - View Lifecycle

    override func viewDidLoad() {
        super.viewDidLoad()

        view.backgroundColor = .ZonaRosa.background
    }

    // MARK: - Orientation

    override var supportedInterfaceOrientations: UIInterfaceOrientationMask {
        return UIDevice.current.isIPad ? .all : .portrait
    }
}
