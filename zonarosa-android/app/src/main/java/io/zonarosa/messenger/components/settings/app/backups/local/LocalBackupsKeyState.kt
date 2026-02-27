/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.components.settings.app.backups.local

import io.zonarosa.core.models.AccountEntropyPool
import io.zonarosa.messenger.components.settings.app.backups.remote.BackupKeySaveState
import io.zonarosa.messenger.keyvalue.ZonaRosaStore

data class LocalBackupsKeyState(
  val accountEntropyPool: AccountEntropyPool = ZonaRosaStore.account.accountEntropyPool,
  val keySaveState: BackupKeySaveState? = null
)
