//
// Copyright 2021 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import ZonaRosaServiceKit
import ZonaRosaUI

/// Namespace for logic around ZonaRosa Dot Me links pointing to a user's phone
/// number. These URLs look like `{https,sgnl}://zonarosa.me/#p/<e164>`.
class ZonaRosaDotMePhoneNumberLink {
    private static let pattern = try! NSRegularExpression(pattern: "^(?:https|\(UrlOpener.Constants.sgnlPrefix))://zonarosa.me/#p/(\\+[0-9]+)$", options: [])

    static func isPossibleUrl(_ url: URL) -> Bool {
        pattern.hasMatch(input: url.absoluteString.lowercased())
    }

    @MainActor
    static func openChat(url: URL, fromViewController: UIViewController) {
        open(url: url, fromViewController: fromViewController) { address in
            AssertIsOnMainThread()
            ZonaRosaApp.shared.presentConversationForAddress(address, action: .compose, animated: true)
        }
    }

    @MainActor
    private static func open(url: URL, fromViewController: UIViewController, block: @escaping (ZonaRosaServiceAddress) -> Void) {
        guard let phoneNumber = pattern.parseFirstMatch(inText: url.absoluteString.lowercased()) else { return }

        ModalActivityIndicatorViewController.present(
            fromViewController: fromViewController,
            canCancel: true,
            asyncBlock: { modal in
                do {
                    let zonarosaRecipients = try await SSKEnvironment.shared.contactDiscoveryManagerRef.lookUp(phoneNumbers: [phoneNumber], mode: .oneOffUserRequest)
                    modal.dismissIfNotCanceled {
                        guard let recipient = zonarosaRecipients.first else {
                            RecipientPickerViewController.presentSMSInvitationSheet(
                                for: phoneNumber,
                                fromViewController: fromViewController,
                            )
                            return
                        }
                        block(recipient.address)
                    }
                } catch {
                    modal.dismissIfNotCanceled {
                        OWSActionSheets.showErrorAlert(message: error.userErrorDescription)
                    }
                }
            },
        )
    }
}
