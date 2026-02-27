//
// Copyright 2020 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import ZonaRosaServiceKit
import ZonaRosaUI

class FlagsViewController: OWSTableViewController2 {
    override func viewDidLoad() {
        super.viewDidLoad()

        title = "Remote Configs"

        updateTableContents()
    }

    func updateTableContents() {
        let contents = OWSTableContents()
        contents.add(buildSection(title: "Remote Config", flagMap: RemoteConfig.current.debugDescriptions()))
        self.contents = contents
    }

    func buildSection(title: String, flagMap: [String: String]) -> OWSTableSection {
        let section = OWSTableSection()
        section.headerTitle = title

        for (key, value) in flagMap.sorted(by: { $0.key < $1.key }) {
            section.add(OWSTableItem(customCellBlock: {
                return OWSTableItem.buildCell(itemName: value, subtitle: key)
            }))
        }

        return section
    }
}
