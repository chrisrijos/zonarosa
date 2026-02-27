//
// Copyright 2023 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import ZonaRosaServiceKit

extension BadgeGiftingConfirmationViewController {
    func startCreditOrDebitCard() {
        guard let navigationController else {
            owsFail("[Gifting] Cannot open credit/debit card screen if we're not in a navigation controller")
        }

        let vc = DonationPaymentDetailsViewController(
            donationAmount: price,
            donationMode: .gift(thread: thread, messageText: messageText),
            // Gifting does not support bank transfers
            paymentMethod: .card,
        ) { [weak self] error in
            guard let self else { return }

            guard let error else {
                self.didCompleteDonation()
                return
            }

            guard let sendGiftError = error as? DonationViewsUtil.Gifts.SendGiftError else {
                owsFail("Non-gift error in gifting flow!")
            }

            DonationViewsUtil.Gifts.presentErrorSheetIfApplicable(for: sendGiftError)
        }

        navigationController.pushViewController(vc, animated: true)
    }
}
