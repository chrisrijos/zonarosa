package io.zonarosa.service.api.messages.multidevice

import io.zonarosa.core.models.AccountEntropyPool
import io.zonarosa.core.models.MasterKey
import io.zonarosa.core.models.backup.MediaRootBackupKey
import io.zonarosa.core.models.storageservice.StorageKey

data class KeysMessage(
  val storageService: StorageKey?,
  val master: MasterKey?,
  val accountEntropyPool: AccountEntropyPool?,
  val mediaRootBackupKey: MediaRootBackupKey?
)
