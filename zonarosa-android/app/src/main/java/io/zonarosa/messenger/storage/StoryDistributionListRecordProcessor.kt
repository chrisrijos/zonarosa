package io.zonarosa.messenger.storage

import io.zonarosa.core.util.StringUtil
import io.zonarosa.core.util.UuidUtil
import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.database.RecipientTable
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.service.api.push.DistributionId
import io.zonarosa.service.api.storage.ZonaRosaStoryDistributionListRecord
import io.zonarosa.service.api.storage.StorageId
import io.zonarosa.service.api.storage.toZonaRosaStoryDistributionListRecord
import io.zonarosa.service.api.util.OptionalUtil.asOptional
import java.io.IOException
import java.util.Optional

/**
 * Record processor for [ZonaRosaStoryDistributionListRecord].
 * Handles merging and updating our local store when processing remote dlist storage records.
 */
class StoryDistributionListRecordProcessor : DefaultStorageRecordProcessor<ZonaRosaStoryDistributionListRecord>() {

  companion object {
    private val TAG = Log.tag(StoryDistributionListRecordProcessor::class.java)
  }

  private var haveSeenMyStory = false

  /**
   * At a minimum, we require:
   *
   *  - A valid identifier
   *  - A non-visually-empty name field OR a deleted at timestamp
   */
  override fun isInvalid(remote: ZonaRosaStoryDistributionListRecord): Boolean {
    val remoteUuid = UuidUtil.parseOrNull(remote.proto.identifier)
    if (remoteUuid == null) {
      Log.d(TAG, "Bad distribution list identifier -- marking as invalid")
      return true
    }

    val isMyStory = remoteUuid == DistributionId.MY_STORY.asUuid()
    if (haveSeenMyStory && isMyStory) {
      Log.w(TAG, "Found an additional MyStory record -- marking as invalid")
      return true
    }

    haveSeenMyStory = haveSeenMyStory or isMyStory

    if (remote.proto.deletedAtTimestamp > 0L) {
      if (isMyStory) {
        Log.w(TAG, "Refusing to delete My Story -- marking as invalid")
        return true
      } else {
        return false
      }
    }

    if (StringUtil.isVisuallyEmpty(remote.proto.name)) {
      Log.d(TAG, "Bad distribution list name (visually empty) -- marking as invalid")
      return true
    }

    return false
  }

  override fun getMatching(remote: ZonaRosaStoryDistributionListRecord, keyGenerator: StorageKeyGenerator): Optional<ZonaRosaStoryDistributionListRecord> {
    Log.d(TAG, "Attempting to get matching record...")
    val matching = ZonaRosaDatabase.distributionLists.getRecipientIdForSyncRecord(remote)
    if (matching == null && UuidUtil.parseOrThrow(remote.proto.identifier) == DistributionId.MY_STORY.asUuid()) {
      Log.e(TAG, "Cannot find matching database record for My Story.")
      throw MyStoryDoesNotExistException()
    }

    if (matching != null) {
      Log.d(TAG, "Found a matching RecipientId for the distribution list...")
      val recordForSync = ZonaRosaDatabase.recipients.getRecordForSync(matching)
      if (recordForSync == null) {
        Log.e(TAG, "Could not find a record for the recipient id in the recipient table")
        throw IllegalStateException("Found matching recipient but couldn't generate record for sync.")
      }

      if (recordForSync.recipientType.id != RecipientTable.RecipientType.DISTRIBUTION_LIST.id) {
        Log.d(TAG, "Record has an incorrect group type.")
        throw InvalidGroupTypeException()
      }

      return StorageSyncModels.localToRemoteRecord(recordForSync).let { it.proto.storyDistributionList!!.toZonaRosaStoryDistributionListRecord(it.id) }.asOptional()
    } else {
      Log.d(TAG, "Could not find a matching record. Returning an empty.")
      return Optional.empty()
    }
  }

  override fun merge(remote: ZonaRosaStoryDistributionListRecord, local: ZonaRosaStoryDistributionListRecord, keyGenerator: StorageKeyGenerator): ZonaRosaStoryDistributionListRecord {
    val merged = ZonaRosaStoryDistributionListRecord.newBuilder(remote.serializedUnknowns).apply {
      identifier = remote.proto.identifier
      name = remote.proto.name
      recipientServiceIds = remote.proto.recipientServiceIds
      deletedAtTimestamp = remote.proto.deletedAtTimestamp
      allowsReplies = remote.proto.allowsReplies
      isBlockList = remote.proto.isBlockList
      recipientServiceIdsBinary = remote.proto.recipientServiceIdsBinary
    }.build().toZonaRosaStoryDistributionListRecord(StorageId.forStoryDistributionList(keyGenerator.generate()))

    val matchesRemote = doParamsMatch(remote, merged)
    val matchesLocal = doParamsMatch(local, merged)

    return if (matchesRemote) {
      remote
    } else if (matchesLocal) {
      local
    } else {
      merged
    }
  }

  @Throws(IOException::class)
  override fun insertLocal(record: ZonaRosaStoryDistributionListRecord) {
    ZonaRosaDatabase.distributionLists.applyStorageSyncStoryDistributionListInsert(record)
  }

  override fun updateLocal(update: StorageRecordUpdate<ZonaRosaStoryDistributionListRecord>) {
    ZonaRosaDatabase.distributionLists.applyStorageSyncStoryDistributionListUpdate(update)
  }

  override fun compare(o1: ZonaRosaStoryDistributionListRecord, o2: ZonaRosaStoryDistributionListRecord): Int {
    return if (o1.proto.identifier == o2.proto.identifier) {
      0
    } else {
      1
    }
  }

  /**
   * Thrown when the RecipientSettings object for a given distribution list is not the
   * correct group type (4).
   */
  private class InvalidGroupTypeException : RuntimeException()

  /**
   * Thrown when we try to ge the matching record for the "My Story" distribution ID but
   * it isn't in the database.
   */
  private class MyStoryDoesNotExistException : RuntimeException()
}
