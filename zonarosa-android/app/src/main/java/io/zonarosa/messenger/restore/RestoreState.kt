/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.restore

import android.content.Intent
import android.net.Uri
import io.zonarosa.messenger.restore.transferorrestore.BackupRestorationType

/**
 * Shared state holder for the restore flow.
 */
data class RestoreState(val restorationType: BackupRestorationType = BackupRestorationType.LOCAL_BACKUP, val backupFile: Uri? = null, val nextIntent: Intent? = null)
