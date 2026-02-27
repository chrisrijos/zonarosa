//
// Copyright 2026 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import UIKit

/// A `UITableViewDiffableDataSource` that exposes hooks into various
/// `UITableViewDataSource` APIs that callers may be interested in.
public class OWSTableViewDiffableDataSource<
    SectionIdentifier: Hashable,
    ItemIdentifier: Hashable,
>: UITableViewDiffableDataSource<
    SectionIdentifier,
    ItemIdentifier,
> {

    public var canMoveRow: ((_ indexPath: IndexPath) -> Bool)?
    public var didMoveRow: ((_ sourceIndexPath: IndexPath, _ destinationIndexPath: IndexPath) -> Void)?

    override public func tableView(
        _ tableView: UITableView,
        canMoveRowAt indexPath: IndexPath,
    ) -> Bool {
        canMoveRow?(indexPath) ?? true
    }

    override public func tableView(
        _ tableView: UITableView,
        moveRowAt sourceIndexPath: IndexPath,
        to destinationIndexPath: IndexPath,
    ) {
        didMoveRow?(sourceIndexPath, destinationIndexPath)
    }
}
