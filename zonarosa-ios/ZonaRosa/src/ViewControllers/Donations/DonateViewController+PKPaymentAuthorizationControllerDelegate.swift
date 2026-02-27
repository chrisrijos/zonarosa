//
// Copyright 2022 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import PassKit

extension DonateViewController: PKPaymentAuthorizationControllerDelegate {
    func paymentAuthorizationControllerDidFinish(_ controller: PKPaymentAuthorizationController) {
        controller.dismiss()
    }

    func paymentAuthorizationController(
        _ controller: PKPaymentAuthorizationController,
        didAuthorizePayment payment: PKPayment,
        handler: @escaping (PKPaymentAuthorizationResult) -> Void,
    ) {
        switch state.donateMode {
        case .oneTime:
            paymentAuthorizationControllerForOneTime(
                controller,
                didAuthorizePayment: payment,
                handler: handler,
            )
        case .monthly:
            paymentAuthorizationControllerForMonthly(
                controller,
                didAuthorizePayment: payment,
                handler: handler,
            )
        }
    }
}
