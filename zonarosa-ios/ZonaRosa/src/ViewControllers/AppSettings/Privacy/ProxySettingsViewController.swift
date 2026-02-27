//
// Copyright 2022 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import ZonaRosaServiceKit
import ZonaRosaUI

class ProxySettingsViewController: OWSTableViewController2 {
    private var useProxy = ZonaRosaProxy.useProxy

    override func viewDidLoad() {
        super.viewDidLoad()

        shouldAvoidKeyboard = true

        title = OWSLocalizedString(
            "PROXY_SETTINGS_TITLE",
            comment: "Title for the zonarosa proxy settings",
        )

        updateTableContents()
        updateNavigationBar()
    }

    private var hasPendingChanges: Bool {
        useProxy != ZonaRosaProxy.useProxy || host != ZonaRosaProxy.host
    }

    private var host: String? {
        hostTextField.text?.nilIfEmpty
    }

    private func updateNavigationBar() {
        if navigationController?.viewControllers.count == 1 {
            navigationItem.leftBarButtonItem = .cancelButton(
                dismissingFrom: self,
                hasUnsavedChanges: { [weak self] in self?.hasPendingChanges },
            )
        }

        navigationItem.rightBarButtonItem = .systemItem(.save) { [weak self] in
            self?.didTapSave()
        }
        navigationItem.rightBarButtonItem?.isEnabled = hasPendingChanges
    }

    private lazy var hostTextField: UITextField = {
        let textField = UITextField()

        textField.text = ZonaRosaProxy.host
        textField.font = .dynamicTypeBody
        textField.backgroundColor = .clear
        textField.placeholder = OWSLocalizedString(
            "PROXY_PLACEHOLDER",
            comment: "Placeholder text for zonarosa proxy host",
        )
        textField.returnKeyType = .done
        textField.autocorrectionType = .no
        textField.autocapitalizationType = .none
        textField.textContentType = .URL
        textField.keyboardType = .URL
        textField.delegate = self
        textField.addTarget(self, action: #selector(textFieldDidChange), for: .editingChanged)

        return textField
    }()

    override func themeDidChange() {
        super.themeDidChange()

        updateTableContents()
    }

    private func updateTableContents() {
        let contents = OWSTableContents()
        defer { self.contents = contents }

        let useProxy = self.useProxy

        let useProxySection = OWSTableSection()
        useProxySection.footerAttributedTitle = .composed(of: [
            OWSLocalizedString("USE_PROXY_EXPLANATION", comment: "Explanation of when you should use a zonarosa proxy"),
            " ",
            CommonStrings.learnMore.styled(with: .link(URL.Support.proxies)),
        ])
        .styled(with: defaultFooterTextStyle)

        useProxySection.add(.switch(
            withText: OWSLocalizedString("USE_PROXY_BUTTON", comment: "Button to activate the zonarosa proxy"),
            isOn: { [weak self] in
                self?.useProxy ?? false
            },
            target: self,
            selector: #selector(didToggleUseProxy),
        ))
        contents.add(useProxySection)

        let proxyAddressSection = OWSTableSection()
        proxyAddressSection.headerAttributedTitle = OWSLocalizedString("PROXY_ADDRESS", comment: "The title for the address of the zonarosa proxy")
            .styled(
                with: .color(defaultHeaderTextColor.withAlphaComponent(useProxy ? 1 : 0.25)),
                .font(Self.defaultHeaderFont),
            )
        proxyAddressSection.add(.init(
            customCellBlock: { [weak self] in
                let cell = OWSTableItem.newCell()
                cell.selectionStyle = .none
                guard let self else { return cell }

                cell.contentView.addSubview(self.hostTextField)
                self.hostTextField.autoPinEdgesToSuperviewMargins()

                if !useProxy {
                    cell.isUserInteractionEnabled = false
                    cell.contentView.alpha = 0.25
                }

                return cell
            },
            actionBlock: {},
        ))
        contents.add(proxyAddressSection)

        let shareSection = OWSTableSection()
        shareSection.add(.init(
            customCellBlock: {
                let cell = OWSTableItem.buildImageCell(image: Theme.iconImage(.buttonShare), itemName: CommonStrings.shareButton)
                cell.selectionStyle = .none

                if !useProxy {
                    cell.isUserInteractionEnabled = false
                    cell.contentView.alpha = 0.25
                }

                return cell
            },
            actionBlock: { [weak self] in
                guard let self else { return }
                guard !self.notifyForInvalidHostIfNecessary() else { return }
                AttachmentSharing.showShareUI(for: URL(string: "https://zonarosa.tube/#\(self.host ?? "")")!, sender: self.view)
            },
        ))
        contents.add(shareSection)
    }

    @objc
    private func didToggleUseProxy(_ sender: UISwitch) {
        useProxy = sender.isOn
        updateTableContents()
        updateNavigationBar()
    }

    @objc
    private func textFieldDidChange() {
        updateNavigationBar()
    }

    private func notifyForInvalidHostIfNecessary() -> Bool {
        guard !ZonaRosaProxy.isValidProxyFragment(host) else { return false }

        // allow saving an empty host when the proxy is off
        if !useProxy, host == nil { return false }

        presentToast(text: OWSLocalizedString("INVALID_PROXY_HOST_ERROR", comment: "The provided proxy host address is not valid"))

        return true
    }

    private func didTapSave() {
        hostTextField.resignFirstResponder()

        guard !notifyForInvalidHostIfNecessary() else { return }

        SSKEnvironment.shared.databaseStorageRef.write { transaction in
            ZonaRosaProxy.setProxyHost(host: self.host, useProxy: self.useProxy, transaction: transaction)
        }
        updateNavigationBar()

        guard useProxy else {
            if navigationController?.viewControllers.count == 1 {
                dismiss(animated: true)
            }
            return
        }

        ModalActivityIndicatorViewController.present(fromViewController: self, canCancel: true, asyncBlock: { modal in
            let connected = await self.checkConnection()

            modal.dismiss {
                if connected {
                    if self.navigationController?.viewControllers.count == 1 {
                        self.presentingViewController?.presentToast(text: OWSLocalizedString("PROXY_CONNECTED_SUCCESSFULLY", comment: "The provided proxy connected successfully"))
                        self.dismiss(animated: true)
                    } else {
                        self.presentToast(text: OWSLocalizedString("PROXY_CONNECTED_SUCCESSFULLY", comment: "The provided proxy connected successfully"))
                    }
                } else {
                    if !modal.wasCancelled {
                        self.presentToast(text: OWSLocalizedString("PROXY_FAILED_TO_CONNECT", comment: "The provided proxy couldn't connect"))
                    }
                    SSKEnvironment.shared.databaseStorageRef.write { transaction in
                        ZonaRosaProxy.setProxyHost(host: self.host, useProxy: false, transaction: transaction)
                    }
                    self.updateTableContents()
                    self.updateNavigationBar()
                }
            }
        })
    }

    private func checkConnection() async -> Bool {
        return await ProxyConnectionChecker(chatConnectionManager: DependenciesBridge.shared.chatConnectionManager).checkConnection()
    }

    var shouldCancelNavigationBack: Bool {
        if hasPendingChanges {
            OWSActionSheets.showPendingChangesActionSheet { [weak self] in
                self?.navigationController?.popViewController(animated: true)
            }
            return true
        } else {
            return false
        }
    }
}

extension ProxySettingsViewController: UITextFieldDelegate {
    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        if hasPendingChanges {
            didTapSave()
        } else {
            textField.resignFirstResponder()
        }
        return false
    }
}
