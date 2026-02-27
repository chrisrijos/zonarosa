package io.zonarosa.messenger.storage

import okio.ByteString
import okio.ByteString.Companion.toByteString
import io.zonarosa.core.models.ServiceId.ACI
import io.zonarosa.core.models.ServiceId.PNI
import io.zonarosa.core.util.isEmpty
import io.zonarosa.core.util.isNotEmpty
import io.zonarosa.core.util.logging.Log
import io.zonarosa.core.util.nullIfBlank
import io.zonarosa.core.util.nullIfEmpty
import io.zonarosa.messenger.crypto.ProfileKeyUtil
import io.zonarosa.messenger.database.RecipientTable
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.database.model.RecipientRecord
import io.zonarosa.messenger.jobs.RetrieveProfileJob.Companion.enqueue
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.recipients.Recipient.Companion.trustedPush
import io.zonarosa.messenger.recipients.RecipientId
import io.zonarosa.messenger.storage.StorageSyncModels.localToRemoteRecord
import io.zonarosa.messenger.util.RemoteConfig
import io.zonarosa.service.api.storage.ZonaRosaContactRecord
import io.zonarosa.service.api.storage.StorageId
import io.zonarosa.service.api.storage.zonarosaAci
import io.zonarosa.service.api.storage.zonarosaPni
import io.zonarosa.service.api.storage.toZonaRosaContactRecord
import io.zonarosa.service.internal.storage.protos.ContactRecord.IdentityState
import java.io.IOException
import java.util.Optional
import java.util.regex.Pattern

/**
 * Record processor for [ZonaRosaContactRecord].
 * Handles merging and updating our local store when processing remote contact storage records.
 */
class ContactRecordProcessor(
  private val selfAci: ACI?,
  private val selfPni: PNI?,
  private val selfE164: String?,
  private val recipientTable: RecipientTable
) : DefaultStorageRecordProcessor<ZonaRosaContactRecord>() {

  companion object {
    private val TAG = Log.tag(ContactRecordProcessor::class.java)

    private val E164_PATTERN: Pattern = Pattern.compile("^\\+[1-9]\\d{6,18}$")

    private fun isValidE164(value: String): Boolean {
      return E164_PATTERN.matcher(value).matches()
    }
  }

  private var rotateProfileKeyOnBlock = true

  constructor() : this(
    selfAci = ZonaRosaStore.account.aci,
    selfPni = ZonaRosaStore.account.pni,
    selfE164 = ZonaRosaStore.account.e164,
    recipientTable = ZonaRosaDatabase.recipients
  )

  /**
   * For contact records specifically, we have some extra work that needs to be done before we process all of the records.
   *
   * We have to find all unregistered ACI-only records and split them into two separate contact rows locally, if necessary.
   * The reasons are nuanced, but the TL;DR is that we want to split unregistered users into separate rows so that a user
   * could re-register and get a different ACI.
   */
  @Throws(IOException::class)
  override fun process(remoteRecords: Collection<ZonaRosaContactRecord>, keyGenerator: StorageKeyGenerator) {
    val unregisteredAciOnly: MutableList<ZonaRosaContactRecord> = ArrayList()

    for (remoteRecord in remoteRecords) {
      if (isInvalid(remoteRecord)) {
        continue
      }

      if (remoteRecord.proto.unregisteredAtTimestamp > 0 && remoteRecord.proto.zonarosaAci != null && remoteRecord.proto.zonarosaPni == null && remoteRecord.proto.e164.isBlank()) {
        unregisteredAciOnly.add(remoteRecord)
      }
    }

    if (unregisteredAciOnly.size > 0) {
      for (aciOnly in unregisteredAciOnly) {
        ZonaRosaDatabase.recipients.splitForStorageSyncIfNecessary(aciOnly.proto.zonarosaAci!!)
      }
    }

    super.process(remoteRecords, keyGenerator)
  }

  /**
   * Error cases:
   * - You can't have a contact record without an ACI or PNI.
   * - You can't have a contact record for yourself. That should be an account record.
   *
   * Note: This method could be written more succinctly, but the logs are useful :)
   */
  override fun isInvalid(remote: ZonaRosaContactRecord): Boolean {
    val hasAci = remote.proto.zonarosaAci?.isValid == true
    val hasPni = remote.proto.zonarosaPni?.isValid == true

    if (!hasAci && !hasPni) {
      Log.w(TAG, "Found a ContactRecord with neither an ACI nor a PNI -- marking as invalid.")
      return true
    } else if (
      selfAci != null &&
      selfAci == remote.proto.zonarosaAci ||
      (selfPni != null && selfPni == remote.proto.zonarosaPni) ||
      (selfE164 != null && remote.proto.e164.isNotBlank() && remote.proto.e164 == selfE164)
    ) {
      Log.w(TAG, "Found a ContactRecord for ourselves -- marking as invalid.")
      return true
    } else if (remote.proto.e164.isNotBlank() && !isValidE164(remote.proto.e164)) {
      Log.w(TAG, "Found a record with an invalid E164. Marking as invalid.")
      return true
    } else {
      return false
    }
  }

  override fun getMatching(remote: ZonaRosaContactRecord, keyGenerator: StorageKeyGenerator): Optional<ZonaRosaContactRecord> {
    var found: Optional<RecipientId> = remote.proto.zonarosaAci?.let { recipientTable.getByAci(it) } ?: Optional.empty()

    if (found.isEmpty && remote.proto.e164.isNotBlank()) {
      found = recipientTable.getByE164(remote.proto.e164)
    }

    if (found.isEmpty && remote.proto.zonarosaPni != null) {
      found = recipientTable.getByPni(remote.proto.zonarosaPni!!)
    }

    return found
      .map { recipientTable.getRecordForSync(it)!! }
      .map { settings: RecipientRecord ->
        if (settings.storageId != null) {
          return@map localToRemoteRecord(settings)
        } else {
          Log.w(TAG, "Newly discovering a registered user via storage service. Saving a storageId for them.")
          recipientTable.updateStorageId(settings.id, keyGenerator.generate())

          val updatedSettings = recipientTable.getRecordForSync(settings.id)!!
          return@map localToRemoteRecord(updatedSettings)
        }
      }
      .map { record -> ZonaRosaContactRecord(record.id, record.proto.contact!!) }
  }

  override fun merge(remote: ZonaRosaContactRecord, local: ZonaRosaContactRecord, keyGenerator: StorageKeyGenerator): ZonaRosaContactRecord {
    val mergedProfileGivenName: String
    val mergedProfileFamilyName: String

    val localAci = local.proto.zonarosaAci
    val localPni = local.proto.zonarosaPni

    val remoteAci = remote.proto.zonarosaAci
    val remotePni = remote.proto.zonarosaPni

    if (remote.proto.givenName.isNotBlank() || remote.proto.familyName.isNotBlank()) {
      mergedProfileGivenName = remote.proto.givenName
      mergedProfileFamilyName = remote.proto.familyName
    } else {
      mergedProfileGivenName = local.proto.givenName
      mergedProfileFamilyName = local.proto.familyName
    }

    val mergedIdentityState: IdentityState
    val mergedIdentityKey: ByteArray?

    if ((remote.proto.identityState != local.proto.identityState && remote.proto.identityKey.isNotEmpty()) ||
      (remote.proto.identityKey.isNotEmpty() && local.proto.identityKey.isEmpty()) ||
      (remote.proto.identityKey.isNotEmpty() && local.proto.unregisteredAtTimestamp > 0)
    ) {
      mergedIdentityState = remote.proto.identityState
      mergedIdentityKey = remote.proto.identityKey.takeIf { it.isNotEmpty() }?.toByteArray()
    } else {
      mergedIdentityState = local.proto.identityState
      mergedIdentityKey = local.proto.identityKey.takeIf { it.isNotEmpty() }?.toByteArray()
    }

    if (localAci != null && mergedIdentityKey != null && remote.proto.identityKey.isNotEmpty() && !mergedIdentityKey.contentEquals(remote.proto.identityKey.toByteArray())) {
      Log.w(TAG, "The local and remote identity keys do not match for " + localAci + ". Enqueueing a profile fetch.")
      enqueue(trustedPush(localAci, localPni, local.proto.e164).id, true)
    }

    val mergedPni: PNI?
    val mergedE164: String?

    val e164sMatchButPnisDont = local.proto.e164.isNotBlank() &&
      local.proto.e164 == remote.proto.e164 &&
      localPni != null &&
      remotePni != null &&
      localPni != remotePni

    val pnisMatchButE164sDont = localPni != null &&
      localPni == remotePni &&
      local.proto.e164.isNotBlank() &&
      remote.proto.e164.isNotBlank() &&
      local.proto.e164 != remote.proto.e164

    if (e164sMatchButPnisDont) {
      Log.w(TAG, "Matching E164s, but the PNIs differ! Trusting our local pair.")
      // TODO [pnp] Schedule CDS fetch?
      mergedPni = localPni
      mergedE164 = local.proto.e164
    } else if (pnisMatchButE164sDont) {
      Log.w(TAG, "Matching PNIs, but the E164s differ! Trusting our local pair.")
      // TODO [pnp] Schedule CDS fetch?
      mergedPni = localPni
      mergedE164 = local.proto.e164
    } else {
      mergedPni = remotePni ?: localPni
      mergedE164 = remote.proto.e164.nullIfBlank() ?: local.proto.e164.nullIfBlank()
    }

    val merged = ZonaRosaContactRecord.newBuilder(remote.serializedUnknowns).apply {
      e164 = mergedE164 ?: ""
      aci = local.proto.aci.nullIfBlank() ?: remote.proto.aci
      pni = mergedPni?.toStringWithoutPrefix() ?: ""
      givenName = mergedProfileGivenName
      familyName = mergedProfileFamilyName
      profileKey = remote.proto.profileKey.nullIfEmpty()?.takeIf { ProfileKeyUtil.profileKeyOrNull(it.toByteArray()) != null } ?: local.proto.profileKey
      username = remote.proto.username.nullIfBlank() ?: local.proto.username
      identityState = mergedIdentityState
      identityKey = mergedIdentityKey?.toByteString() ?: ByteString.EMPTY
      blocked = remote.proto.blocked
      whitelisted = remote.proto.whitelisted
      archived = remote.proto.archived
      markedUnread = remote.proto.markedUnread
      mutedUntilTimestamp = remote.proto.mutedUntilTimestamp
      hideStory = remote.proto.hideStory
      unregisteredAtTimestamp = remote.proto.unregisteredAtTimestamp
      hidden = remote.proto.hidden
      systemGivenName = if (ZonaRosaStore.account.isPrimaryDevice) local.proto.systemGivenName else remote.proto.systemGivenName
      systemFamilyName = if (ZonaRosaStore.account.isPrimaryDevice) local.proto.systemFamilyName else remote.proto.systemFamilyName
      systemNickname = remote.proto.systemNickname
      nickname = remote.proto.nickname
      pniSignatureVerified = remote.proto.pniSignatureVerified || local.proto.pniSignatureVerified
      note = remote.proto.note.nullIfBlank() ?: ""
      avatarColor = if (ZonaRosaStore.account.isPrimaryDevice) local.proto.avatarColor else remote.proto.avatarColor
      aciBinary = if (RemoteConfig.useBinaryId) local.proto.aciBinary.nullIfEmpty() ?: remote.proto.aciBinary else ByteString.EMPTY
      pniBinary = if (RemoteConfig.useBinaryId) mergedPni?.toByteStringWithoutPrefix() ?: byteArrayOf().toByteString() else ByteString.EMPTY
    }.build().toZonaRosaContactRecord(StorageId.forContact(keyGenerator.generate()))

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

  override fun insertLocal(record: ZonaRosaContactRecord) {
    val profileKeyRotated = recipientTable.applyStorageSyncContactInsert(record, rotateProfileKeyOnBlock)
    if (profileKeyRotated) {
      rotateProfileKeyOnBlock = false
    }
  }

  override fun updateLocal(update: StorageRecordUpdate<ZonaRosaContactRecord>) {
    val profileKeyRotated = recipientTable.applyStorageSyncContactUpdate(update, rotateProfileKeyOnBlock)
    if (profileKeyRotated) {
      rotateProfileKeyOnBlock = false
    }
  }

  override fun compare(lhs: ZonaRosaContactRecord, rhs: ZonaRosaContactRecord): Int {
    return if (
      (lhs.proto.zonarosaAci != null && lhs.proto.aci == rhs.proto.aci && lhs.proto.aciBinary == rhs.proto.aciBinary) ||
      (lhs.proto.e164.isNotBlank() && lhs.proto.e164 == rhs.proto.e164) ||
      (lhs.proto.zonarosaPni != null && lhs.proto.pni == rhs.proto.pni && lhs.proto.pniBinary == rhs.proto.pniBinary)
    ) {
      0
    } else {
      1
    }
  }
}
