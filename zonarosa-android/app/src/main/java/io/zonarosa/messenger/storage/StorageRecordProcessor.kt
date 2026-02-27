package io.zonarosa.messenger.storage

import io.zonarosa.service.api.storage.ZonaRosaRecord
import java.io.IOException

/**
 * Handles processing a remote record, which involves applying any local changes that need to be
 * made based on the remote records.
 */
interface StorageRecordProcessor<E : ZonaRosaRecord<*>> {
  @Throws(IOException::class)
  fun process(remoteRecords: Collection<E>, keyGenerator: StorageKeyGenerator)
}
