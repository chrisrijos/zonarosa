//
// Copyright 2025 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import LibZonaRosaClient
import XCTest

@testable import ZonaRosaServiceKit

class BackupSettingsStoreTests: XCTestCase {
    private var db: InMemoryDB!
    private var backupSettingsStore = BackupSettingsStore()

    override func setUp() {
        super.setUp()
        db = InMemoryDB()
    }

    func testLastAndFirstBackupDate() throws {
        var lastBackupDetails = db.read { tx in
            backupSettingsStore.lastBackupDetails(tx: tx)
        }
        XCTAssertNil(lastBackupDetails, "Last backup should not be set")

        lastBackupDetails = db.write { tx in
            backupSettingsStore.setLastBackupDetails(Date(), tx: tx)
            return backupSettingsStore.lastBackupDetails(tx: tx)
        }
        XCTAssertNotNil(lastBackupDetails, "Last backup should be set")
        XCTAssertEqual(lastBackupDetails!.date, lastBackupDetails!.firstBackupDate, "First and last backups should be the same")

        lastBackupDetails = db.write { tx in
            backupSettingsStore.setLastBackupDetails(Date(), tx: tx)
            return backupSettingsStore.lastBackupDetails(tx: tx)
        }
        XCTAssertTrue(lastBackupDetails!.firstBackupDate < lastBackupDetails!.date, "First backup should not update after it is first set")
    }

    func testBackupUpdatesRefreshDate() throws {
        var lastBackupRefresh = db.read { tx in
            CronStore(uniqueKey: .refreshBackup).mostRecentDate(tx: tx)
        }
        XCTAssertEqual(lastBackupRefresh, .distantPast, "Last backup should not be set")

        db.write { tx in
            backupSettingsStore.setLastBackupDetails(Date(), tx: tx)
        }

        lastBackupRefresh = db.read { tx in
            CronStore(uniqueKey: .refreshBackup).mostRecentDate(tx: tx)
        }
        XCTAssertNotEqual(lastBackupRefresh, .distantPast, "Last backup should be set")
    }
}

// MARK: -

private extension BackupSettingsStore {
    func setLastBackupDetails(_ date: Date, tx: DBWriteTransaction) {
        setLastBackupDetails(date: date, backupFileSizeBytes: 1, backupMediaSizeBytes: 1, tx: tx)
    }
}
