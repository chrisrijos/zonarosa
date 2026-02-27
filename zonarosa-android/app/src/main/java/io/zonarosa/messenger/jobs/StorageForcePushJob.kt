package io.zonarosa.messenger.jobs

import io.zonarosa.core.util.SqlUtil
import io.zonarosa.core.util.logging.Log
import io.zonarosa.core.util.logging.logI
import io.zonarosa.messenger.components.settings.app.chats.folders.ChatFolderId
import io.zonarosa.messenger.database.ChatFolderTables.ChatFolderTable
import io.zonarosa.messenger.database.NotificationProfileTables
import io.zonarosa.messenger.database.RecipientTable
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobmanager.Job
import io.zonarosa.messenger.jobmanager.impl.NetworkConstraint
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.notifications.profiles.NotificationProfileId
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.recipients.RecipientId
import io.zonarosa.messenger.storage.StorageSyncHelper
import io.zonarosa.messenger.storage.StorageSyncModels
import io.zonarosa.messenger.storage.StorageSyncValidations
import io.zonarosa.messenger.transport.RetryLaterException
import io.zonarosa.service.api.NetworkResult
import io.zonarosa.service.api.push.exceptions.PushNetworkException
import io.zonarosa.service.api.storage.RecordIkm
import io.zonarosa.service.api.storage.ZonaRosaStorageManifest
import io.zonarosa.service.api.storage.ZonaRosaStorageRecord
import io.zonarosa.service.api.storage.StorageId
import io.zonarosa.service.api.storage.StorageServiceRepository
import java.io.IOException
import java.util.Collections
import java.util.concurrent.TimeUnit

/**
 * Forces remote storage to match our local state. This should only be done when we detect that the
 * remote data is badly-encrypted (which should only happen after re-registering without a PIN).
 */
class StorageForcePushJob private constructor(parameters: Parameters) : BaseJob(parameters) {
  companion object {
    const val KEY: String = "StorageForcePushJob"

    private val TAG = Log.tag(StorageForcePushJob::class.java)
  }

  constructor() : this(
    Parameters.Builder().addConstraint(NetworkConstraint.KEY)
      .setQueue(StorageSyncJob.QUEUE_KEY)
      .setMaxInstancesForFactory(1)
      .setLifespan(TimeUnit.DAYS.toMillis(1))
      .build()
  )

  override fun serialize(): ByteArray? = null

  override fun getFactoryKey(): String = KEY

  @Throws(IOException::class, RetryLaterException::class)
  override fun onRun() {
    if (ZonaRosaStore.account.isLinkedDevice) {
      Log.i(TAG, "Only the primary device can force push")
      return
    }

    if (!ZonaRosaStore.account.isRegistered || ZonaRosaStore.account.e164 == null) {
      Log.w(TAG, "User not registered. Skipping.")
      return
    }

    if (Recipient.self().storageId == null) {
      Log.w(TAG, "No storage ID set for self! Skipping.")
      return
    }

    val storageServiceKey = ZonaRosaStore.storageService.storageKey
    val repository = StorageServiceRepository(AppDependencies.storageServiceApi)

    val currentVersion: Long = when (val result = repository.getManifestVersion()) {
      is NetworkResult.Success -> result.result
      is NetworkResult.ApplicationError -> throw result.throwable
      is NetworkResult.NetworkError -> throw result.exception
      is NetworkResult.StatusCodeError -> {
        when (result.code) {
          404 -> 0L.logI(TAG, "No manifest found, defaulting to version 0.")
          else -> throw result.exception
        }
      }
    }
    val oldContactStorageIds: Map<RecipientId, StorageId> = ZonaRosaDatabase.recipients.getContactStorageSyncIdsMap()

    val newVersion = currentVersion + 1
    val newContactStorageIds = generateContactStorageIds(oldContactStorageIds)
    val inserts: MutableList<ZonaRosaStorageRecord> = oldContactStorageIds.keys
      .mapNotNull { ZonaRosaDatabase.recipients.getRecordForSync(it) }
      .filter { it.recipientType != RecipientTable.RecipientType.INDIVIDUAL || (it.aci != null || it.pni != null || it.e164 != null) }
      .map { record -> StorageSyncModels.localToRemoteRecord(record, newContactStorageIds[record.id]!!.raw) }
      .toMutableList()

    val accountRecord = StorageSyncHelper.buildAccountRecord(context, Recipient.self().fresh())
    val allNewStorageIds: MutableList<StorageId> = ArrayList(newContactStorageIds.values)

    inserts.add(accountRecord)
    allNewStorageIds.add(accountRecord.id)

    val oldChatFolderStorageIds = ZonaRosaDatabase.chatFolders.getStorageSyncIdsMap()
    val newChatFolderStorageIds = generateChatFolderStorageIds(oldChatFolderStorageIds)
    val newChatFolderInserts: List<ZonaRosaStorageRecord> = oldChatFolderStorageIds.keys
      .mapNotNull {
        val query = SqlUtil.buildQuery("${ChatFolderTable.CHAT_FOLDER_ID} = ?", it)
        ZonaRosaDatabase.chatFolders.getChatFolder(query)
      }
      .map { record -> StorageSyncModels.localToRemoteRecord(record, newChatFolderStorageIds[record.chatFolderId]!!.raw) }

    inserts.addAll(newChatFolderInserts)
    allNewStorageIds.addAll(newChatFolderStorageIds.values)

    val oldNotificationProfileStorageIds = ZonaRosaDatabase.notificationProfiles.getStorageSyncIdsMap()
    val newNotificationProfileStorageIds = generateNotificationProfileStorageIds(oldNotificationProfileStorageIds)
    val newNotificationProfileInserts: List<ZonaRosaStorageRecord> = oldNotificationProfileStorageIds.keys
      .mapNotNull {
        val query = SqlUtil.buildQuery("${NotificationProfileTables.NotificationProfileTable.NOTIFICATION_PROFILE_ID} = ?", it)
        ZonaRosaDatabase.notificationProfiles.getProfile(query)
      }
      .map { record -> StorageSyncModels.localToRemoteRecord(record, newNotificationProfileStorageIds[record.notificationProfileId]!!.raw) }

    inserts.addAll(newNotificationProfileInserts)
    allNewStorageIds.addAll(newNotificationProfileStorageIds.values)

    Log.i(TAG, "Generating and including a new recordIkm.")
    val recordIkm: RecordIkm = RecordIkm.generate()

    val manifest = ZonaRosaStorageManifest(newVersion, ZonaRosaStore.account.deviceId, recordIkm, allNewStorageIds)
    StorageSyncValidations.validateForcePush(manifest, inserts, Recipient.self().fresh())

    if (newVersion > 1) {
      Log.i(TAG, "Force-pushing data. Inserting ${inserts.size} IDs.")
      when (val result = repository.resetAndWriteStorageRecords(storageServiceKey, manifest, inserts)) {
        StorageServiceRepository.WriteStorageRecordsResult.Success -> Unit
        is StorageServiceRepository.WriteStorageRecordsResult.StatusCodeError -> throw result.exception
        is StorageServiceRepository.WriteStorageRecordsResult.NetworkError -> throw result.exception
        StorageServiceRepository.WriteStorageRecordsResult.ConflictError -> {
          Log.w(TAG, "Hit a conflict. Trying again.")
          throw RetryLaterException()
        }
      }
    } else {
      Log.i(TAG, "First version, normal push. Inserting ${inserts.size} IDs.")
      when (val result = repository.writeStorageRecords(storageServiceKey, manifest, inserts, emptyList())) {
        StorageServiceRepository.WriteStorageRecordsResult.Success -> Unit
        is StorageServiceRepository.WriteStorageRecordsResult.StatusCodeError -> throw result.exception
        is StorageServiceRepository.WriteStorageRecordsResult.NetworkError -> throw result.exception
        is StorageServiceRepository.WriteStorageRecordsResult.ConflictError -> {
          Log.w(TAG, "Hit a conflict. Trying again.")
          throw RetryLaterException()
        }
      }
    }

    Log.i(TAG, "Force push succeeded. Updating local manifest version to: $newVersion")
    ZonaRosaStore.storageService.manifest = manifest
    ZonaRosaStore.svr.masterKeyForInitialDataRestore = null
    ZonaRosaDatabase.recipients.applyStorageIdUpdates(newContactStorageIds)
    ZonaRosaDatabase.recipients.applyStorageIdUpdates(Collections.singletonMap(Recipient.self().id, accountRecord.id))
    ZonaRosaDatabase.chatFolders.applyStorageIdUpdates(newChatFolderStorageIds)
    ZonaRosaDatabase.notificationProfiles.applyStorageIdUpdates(newNotificationProfileStorageIds)
    ZonaRosaDatabase.unknownStorageIds.deleteAll()
  }

  override fun onShouldRetry(e: Exception): Boolean {
    return e is PushNetworkException || e is RetryLaterException
  }

  override fun onFailure() = Unit

  private fun generateContactStorageIds(oldKeys: Map<RecipientId, StorageId>): Map<RecipientId, StorageId> {
    val out: MutableMap<RecipientId, StorageId> = mutableMapOf()

    for ((key, value) in oldKeys) {
      out[key] = value.withNewBytes(StorageSyncHelper.generateKey())
    }

    return out
  }

  private fun generateChatFolderStorageIds(oldKeys: Map<ChatFolderId, StorageId>): Map<ChatFolderId, StorageId> {
    val out: MutableMap<ChatFolderId, StorageId> = mutableMapOf()

    for ((key, value) in oldKeys) {
      out[key] = value.withNewBytes(StorageSyncHelper.generateKey())
    }

    return out
  }

  private fun generateNotificationProfileStorageIds(oldKeys: Map<NotificationProfileId, StorageId>): Map<NotificationProfileId, StorageId> {
    return oldKeys.mapValues { (_, value) ->
      value.withNewBytes(StorageSyncHelper.generateKey())
    }
  }

  class Factory : Job.Factory<StorageForcePushJob?> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): StorageForcePushJob {
      return StorageForcePushJob(parameters)
    }
  }
}
