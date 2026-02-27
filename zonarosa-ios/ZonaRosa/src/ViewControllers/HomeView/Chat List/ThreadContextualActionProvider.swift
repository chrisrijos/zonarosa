//
// Copyright 2021 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import ZonaRosaServiceKit
import ZonaRosaUI

// MARK: -

private struct ThreadContextualAction {
    let style: UIContextualAction.Style
    let color: UIColor
    let imageFilled: UIImage
    let imageStroked: UIImage
    let title: String
    let action: () -> Void
}

// MARK: -

protocol ThreadContextualActionProvider {
    func threadContextualActionShouldCloseThreadIfActive(threadViewModel: ThreadViewModel)
    func threadContextualActionDidComplete()
}

extension ThreadContextualActionProvider where Self: UIViewController {

    func leadingSwipeActionsConfiguration(threadViewModel: ThreadViewModel) -> UISwipeActionsConfiguration? {
        AssertIsOnMainThread()

        let actions: [ThreadContextualAction] = [
            readStateContextualAction(threadViewModel: threadViewModel),
            pinnedStateContextualAction(threadViewModel: threadViewModel),
        ]

        return UISwipeActionsConfiguration(actions: actions.map { action in
            makeUIContextualAction(threadContextualAction: action)
        })
    }

    func trailingSwipeActionsConfiguration(threadViewModel: ThreadViewModel) -> UISwipeActionsConfiguration? {
        AssertIsOnMainThread()

        let actions: [ThreadContextualAction] = [
            archiveStateContextualAction(threadViewModel: threadViewModel),
            deleteContextualAction(threadViewModel: threadViewModel),
            muteStateContextualAction(threadViewModel: threadViewModel),
        ]

        return UISwipeActionsConfiguration(actions: actions.map { action in
            makeUIContextualAction(threadContextualAction: action)
        })
    }

    private func makeUIContextualAction(threadContextualAction action: ThreadContextualAction) -> UIContextualAction {
        return ContextualActionBuilder.makeContextualAction(
            style: action.style,
            color: action.color,
            image: action.imageFilled,
            title: action.title,
            handler: { completion in
                action.action()
                completion(false)
            },
        )
    }

    // MARK: -

    func contextMenuActions(threadViewModel: ThreadViewModel) -> [UIAction] {
        AssertIsOnMainThread()

        let actions: [ThreadContextualAction] = [
            readStateContextualAction(threadViewModel: threadViewModel),
            muteStateContextualAction(threadViewModel: threadViewModel),
            pinnedStateContextualAction(threadViewModel: threadViewModel),
            archiveStateContextualAction(threadViewModel: threadViewModel),
            deleteContextualAction(threadViewModel: threadViewModel),
        ]

        return actions.map { action in
            makeUIAction(threadContextualAction: action)
        }
    }

    private func makeUIAction(
        threadContextualAction action: ThreadContextualAction,
    ) -> UIAction {
        let attributes: UIMenuElement.Attributes = switch action.style {
        case .normal: []
        case .destructive: [.destructive]
        @unknown default: []
        }

        return UIAction(
            title: action.title,
            image: action.imageStroked,
            attributes: attributes,
            handler: { _ in
                action.action()
            },
        )
    }

    // MARK: -

    private func pinnedStateContextualAction(threadViewModel: ThreadViewModel) -> ThreadContextualAction {
        if threadViewModel.isPinned {
            return ThreadContextualAction(
                style: .normal,
                color: UIColor(rgbHex: 0xff990a),
                imageFilled: .pinSlashFill,
                imageStroked: .pinSlash,
                title: CommonStrings.unpinAction,
            ) { [weak self] in
                self?.unpinThread(threadViewModel: threadViewModel)
            }
        } else {
            return ThreadContextualAction(
                style: .normal,
                color: UIColor(rgbHex: 0xff990a),
                imageFilled: .pinFill,
                imageStroked: .pin,
                title: CommonStrings.pinAction,
            ) { [weak self] in
                self?.pinThread(threadViewModel: threadViewModel)
            }
        }
    }

    private func readStateContextualAction(threadViewModel: ThreadViewModel) -> ThreadContextualAction {
        if threadViewModel.hasUnreadMessages {
            return ThreadContextualAction(
                style: .normal,
                color: UIColor.ZonaRosa.ultramarine,
                imageFilled: .chatCheckFill,
                imageStroked: .chatCheck,
                title: CommonStrings.readAction,
            ) { [weak self] in
                self?.markThreadAsRead(threadViewModel: threadViewModel)
            }
        } else {
            return ThreadContextualAction(
                style: .normal,
                color: UIColor.ZonaRosa.ultramarine,
                imageFilled: .chatBadgeFill,
                imageStroked: .chatBadge,
                title: CommonStrings.unreadAction,
            ) { [weak self] in
                self?.markThreadAsUnread(threadViewModel: threadViewModel)
            }
        }
    }

    private func muteStateContextualAction(threadViewModel: ThreadViewModel) -> ThreadContextualAction {
        if threadViewModel.isMuted {
            return ThreadContextualAction(
                style: .normal,
                color: UIColor.ZonaRosa.indigo,
                imageFilled: .bellFill,
                imageStroked: .bell,
                title: CommonStrings.unmuteButton,
                action: { [weak self] in
                    self?.unmuteThread(threadViewModel: threadViewModel)
                },
            )
        } else {
            return ThreadContextualAction(
                style: .normal,
                color: UIColor.ZonaRosa.indigo,
                imageFilled: .bellSlashFill,
                imageStroked: .bellSlash,
                title: CommonStrings.muteButton,
                action: { [weak self] in
                    self?.muteThreadWithSelection(threadViewModel: threadViewModel)
                },
            )
        }
    }

    private func deleteContextualAction(threadViewModel: ThreadViewModel) -> ThreadContextualAction {
        return ThreadContextualAction(
            style: .destructive,
            color: UIColor.ZonaRosa.red,
            imageFilled: .trashFill,
            imageStroked: .trash,
            title: CommonStrings.deleteButton,
        ) { [weak self] in
            self?.deleteThreadWithConfirmation(threadViewModel: threadViewModel)
        }
    }

    private func archiveStateContextualAction(threadViewModel: ThreadViewModel) -> ThreadContextualAction {
        if threadViewModel.isArchived {
            return ThreadContextualAction(
                style: .normal,
                color: Theme.isDarkThemeEnabled ? .ows_gray45 : .ows_gray25,
                imageFilled: .archiveUpFill,
                imageStroked: .archiveUp,
                title: CommonStrings.unarchiveAction,
                action: { [weak self] in
                    self?.toggleThreadIsArchived(threadViewModel: threadViewModel)
                },
            )
        } else {
            return ThreadContextualAction(
                style: .normal,
                color: Theme.isDarkThemeEnabled ? .ows_gray45 : .ows_gray25,
                imageFilled: .archiveFill,
                imageStroked: .archive,
                title: CommonStrings.archiveAction,
                action: { [weak self] in
                    self?.toggleThreadIsArchived(threadViewModel: threadViewModel)
                },
            )
        }
    }

    // MARK: -

    func toggleThreadIsArchived(threadViewModel: ThreadViewModel) {
        AssertIsOnMainThread()

        threadContextualActionShouldCloseThreadIfActive(threadViewModel: threadViewModel)

        SSKEnvironment.shared.databaseStorageRef.write { transaction in
            threadViewModel.associatedData.updateWith(
                isArchived: !threadViewModel.isArchived,
                updateStorageService: true,
                transaction: transaction,
            )
        }

        threadContextualActionDidComplete()
    }

    private func deleteThreadWithConfirmation(threadViewModel: ThreadViewModel) {
        AssertIsOnMainThread()
        let db = DependenciesBridge.shared.db
        let threadSoftDeleteManager = DependenciesBridge.shared.threadSoftDeleteManager

        let alert = ActionSheetController(
            title: OWSLocalizedString(
                "CONVERSATION_DELETE_CONFIRMATION_ALERT_TITLE",
                comment: "Title for the 'conversation delete confirmation' alert.",
            ),
            message: OWSLocalizedString(
                "CONVERSATION_DELETE_CONFIRMATION_ALERT_MESSAGE",
                comment: "Message for the 'conversation delete confirmation' alert.",
            ),
        )
        alert.addAction(ActionSheetAction(
            title: CommonStrings.deleteButton,
            style: .destructive,
        ) { [weak self] _ in
            guard let self else { return }

            threadContextualActionShouldCloseThreadIfActive(threadViewModel: threadViewModel)

            ModalActivityIndicatorViewController.present(
                fromViewController: self,
            ) { [weak self] modal in
                guard let self else { return }

                await db.awaitableWrite { tx in
                    threadSoftDeleteManager.softDelete(
                        threads: [threadViewModel.threadRecord],
                        sendDeleteForMeSyncMessage: true,
                        tx: tx,
                    )
                }

                modal.dismiss {
                    self.threadContextualActionDidComplete()
                }
            }
        })
        alert.addAction(OWSActionSheets.cancelAction)

        presentActionSheet(alert)
    }

    func markThreadAsRead(threadViewModel: ThreadViewModel) {
        AssertIsOnMainThread()

        SSKEnvironment.shared.databaseStorageRef.write { transaction in
            threadViewModel.threadRecord.markAllAsRead(updateStorageService: true, transaction: transaction)
        }
    }

    private func markThreadAsUnread(threadViewModel: ThreadViewModel) {
        AssertIsOnMainThread()

        SSKEnvironment.shared.databaseStorageRef.write { transaction in
            threadViewModel.associatedData.updateWith(isMarkedUnread: true, updateStorageService: true, transaction: transaction)
        }
    }

    private func muteThreadWithSelection(threadViewModel: ThreadViewModel) {
        AssertIsOnMainThread()

        let alert = ActionSheetController(title: OWSLocalizedString(
            "CONVERSATION_MUTE_CONFIRMATION_ALERT_TITLE",
            comment: "Title for the 'conversation mute confirmation' alert.",
        ))
        for (title, seconds) in [
            (OWSLocalizedString("CONVERSATION_MUTE_CONFIRMATION_OPTION_1H", comment: "1 hour"), TimeInterval.hour),
            (OWSLocalizedString("CONVERSATION_MUTE_CONFIRMATION_OPTION_8H", comment: "8 hours"), 8 * TimeInterval.hour),
            (OWSLocalizedString("CONVERSATION_MUTE_CONFIRMATION_OPTION_1D", comment: "1 day"), TimeInterval.day),
            (OWSLocalizedString("CONVERSATION_MUTE_CONFIRMATION_OPTION_1W", comment: "1 week"), TimeInterval.week),
            (OWSLocalizedString("CONVERSATION_MUTE_CONFIRMATION_OPTION_ALWAYS", comment: "Always"), -1),
        ] {
            alert.addAction(ActionSheetAction(title: title, style: .default) { [weak self] _ in
                self?.muteThread(threadViewModel: threadViewModel, duration: seconds)
            })
        }
        alert.addAction(OWSActionSheets.cancelAction)

        presentActionSheet(alert)
    }

    private func muteThread(threadViewModel: ThreadViewModel, duration seconds: TimeInterval) {
        AssertIsOnMainThread()

        SSKEnvironment.shared.databaseStorageRef.write { transaction in
            let timestamp = seconds < 0
                ? ThreadAssociatedData.alwaysMutedTimestamp
                : (seconds == 0 ? 0 : Date.ows_millisecondTimestamp() + UInt64(seconds * 1000))
            threadViewModel.associatedData.updateWith(mutedUntilTimestamp: timestamp, updateStorageService: true, transaction: transaction)
        }
    }

    private func unmuteThread(threadViewModel: ThreadViewModel) {
        AssertIsOnMainThread()

        SSKEnvironment.shared.databaseStorageRef.write { transaction in
            threadViewModel.associatedData.updateWith(mutedUntilTimestamp: 0, updateStorageService: true, transaction: transaction)
        }
    }

    private func pinThread(threadViewModel: ThreadViewModel) {
        AssertIsOnMainThread()

        do {
            try SSKEnvironment.shared.databaseStorageRef.write { transaction in
                try DependenciesBridge.shared.pinnedThreadManager.pinThread(
                    threadViewModel.threadRecord,
                    updateStorageService: true,
                    tx: transaction,
                )
            }
        } catch {
            if case PinnedThreadError.tooManyPinnedThreads = error {
                OWSActionSheets.showActionSheet(title: OWSLocalizedString(
                    "PINNED_CONVERSATION_LIMIT",
                    comment: "An explanation that you have already pinned the maximum number of conversations.",
                ))
            } else {
                owsFailDebug("Error: \(error)")
            }
        }
    }

    private func unpinThread(threadViewModel: ThreadViewModel) {
        AssertIsOnMainThread()

        do {
            try SSKEnvironment.shared.databaseStorageRef.write { transaction in
                try DependenciesBridge.shared.pinnedThreadManager.unpinThread(
                    threadViewModel.threadRecord,
                    updateStorageService: true,
                    tx: transaction,
                )
            }
        } catch {
            owsFailDebug("Error: \(error)")
        }
    }
}
