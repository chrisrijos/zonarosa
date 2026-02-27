//
// Copyright 2022 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import ZonaRosaServiceKit
import ZonaRosaUI

class ArchivedConversationsCell: UITableViewCell, ReusableTableViewCell {
    static let reuseIdentifier = "ArchivedConversationsCell"

    private let label = UILabel()
    private let disclosureImageView = UIImageView(image: UIImage(imageLiteralResourceName: "chevron-right-20"))

    var enabled = true {
        didSet {
            if enabled {
                label.textColor = .ZonaRosa.label
            } else {
                label.textColor = .ZonaRosa.secondaryLabel
            }
            disclosureImageView.tintColor = label.textColor
        }
    }

    // MARK: -

    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)

        commonInit()
    }

    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    private func commonInit() {
        selectionStyle = .none

        var configuration = UIBackgroundConfiguration.clear()
        configuration.backgroundColor = .ZonaRosa.background
        backgroundConfiguration = configuration

        disclosureImageView.tintColor = .ZonaRosa.label
        disclosureImageView.setContentHuggingHigh()
        disclosureImageView.setCompressionResistanceHigh()

        label.text = OWSLocalizedString(
            "HOME_VIEW_ARCHIVED_CONVERSATIONS",
            comment: "Label for 'archived conversations' button.",
        )
        label.textAlignment = .center
        label.font = .dynamicTypeBody

        let stackView = UIStackView(arrangedSubviews: [label, disclosureImageView])
        stackView.axis = .horizontal
        stackView.spacing = 5
        stackView.alignment = .center
        stackView.translatesAutoresizingMaskIntoConstraints = false
        contentView.addSubview(stackView)
        contentView.addConstraints([
            stackView.topAnchor.constraint(equalTo: contentView.layoutMarginsGuide.topAnchor),
            stackView.leadingAnchor.constraint(greaterThanOrEqualTo: contentView.layoutMarginsGuide.leadingAnchor),
            stackView.centerXAnchor.constraint(equalTo: contentView.layoutMarginsGuide.centerXAnchor),
            stackView.bottomAnchor.constraint(equalTo: contentView.layoutMarginsGuide.bottomAnchor),
        ])

        accessibilityIdentifier = "archived_conversations"
        enabled = true
    }

    func configure(enabled: Bool) {
        self.enabled = enabled
        NotificationCenter.default.removeObserver(self)
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(multiSelectionModeDidChange),
            name: MultiSelectState.multiSelectionModeDidChange,
            object: nil,
        )
    }

    override func prepareForReuse() {
        NotificationCenter.default.removeObserver(self)
        super.prepareForReuse()
    }

    @objc
    private func multiSelectionModeDidChange(notification: Notification) {
        AssertIsOnMainThread()

        guard let multiSelectActive = notification.object as? Bool else {
            return
        }
        self.enabled = !multiSelectActive
    }
}
