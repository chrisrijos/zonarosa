//
// Copyright 2023 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation

extension BackupArchive {

    enum Constants {
        /// We reject downloading backup proto files larger than this.
        static let maxDownloadSizeBytes: UInt64 = 100 * 1024 * 1024 // 100 MiB
    }
}
