package io.zonarosa.messenger.storage

import android.content.Context
import androidx.annotation.VisibleForTesting
import okio.ByteString
import okio.ByteString.Companion.toByteString
import io.zonarosa.core.util.Base64.encodeWithPadding
import io.zonarosa.core.util.SqlUtil
import io.zonarosa.core.util.Util
import io.zonarosa.core.util.UuidUtil
import io.zonarosa.core.util.logging.Log
import io.zonarosa.core.util.toByteArray
import io.zonarosa.messenger.backup.v2.MessageBackupTier
import io.zonarosa.messenger.components.settings.app.subscription.InAppPaymentsRepository.getSubscriber
import io.zonarosa.messenger.components.settings.app.subscription.InAppPaymentsRepository.isUserManuallyCancelled
import io.zonarosa.messenger.components.settings.app.subscription.InAppPaymentsRepository.setSubscriber
import io.zonarosa.messenger.database.NotificationProfileTables
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.database.model.InAppPaymentSubscriberRecord
import io.zonarosa.messenger.database.model.RecipientRecord
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobs.RetrieveProfileAvatarJob
import io.zonarosa.messenger.jobs.StorageSyncJob
import io.zonarosa.messenger.keyvalue.AccountValues
import io.zonarosa.messenger.keyvalue.PhoneNumberPrivacyValues.PhoneNumberDiscoverabilityMode
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.notifications.profiles.NotificationProfileId
import io.zonarosa.messenger.payments.Entropy
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.recipients.Recipient.Companion.self
import io.zonarosa.messenger.util.ZonaRosaPreferences
import io.zonarosa.service.api.push.UsernameLinkComponents
import io.zonarosa.service.api.storage.ZonaRosaAccountRecord
import io.zonarosa.service.api.storage.ZonaRosaContactRecord
import io.zonarosa.service.api.storage.ZonaRosaStorageManifest
import io.zonarosa.service.api.storage.ZonaRosaStorageRecord
import io.zonarosa.service.api.storage.StorageId
import io.zonarosa.service.api.storage.safeSetBackupsSubscriber
import io.zonarosa.service.api.storage.safeSetPayments
import io.zonarosa.service.api.storage.safeSetSubscriber
import io.zonarosa.service.api.storage.toZonaRosaAccountRecord
import io.zonarosa.service.api.storage.toZonaRosaStorageRecord
import io.zonarosa.service.internal.storage.protos.AccountRecord
import io.zonarosa.service.internal.storage.protos.OptionalBool
import java.util.Optional
import java.util.concurrent.TimeUnit

object StorageSyncHelper {
  private val TAG = Log.tag(StorageSyncHelper::class.java)

  val KEY_GENERATOR: StorageKeyGenerator = StorageKeyGenerator { Util.getSecretBytes(16) }

  private var keyGenerator = KEY_GENERATOR

  private val REFRESH_INTERVAL = TimeUnit.HOURS.toMillis(2)

  /**
   * Given a list of all the local and remote keys you know about, this will return a result telling
   * you which keys are exclusively remote and which are exclusively local.
   *
   * @param remoteIds All remote keys available.
   * @param localIds  All local keys available.
   * @return An object describing which keys are exclusive to the remote data set and which keys are
   * exclusive to the local data set.
   */
  @JvmStatic
  fun findIdDifference(
    remoteIds: Collection<StorageId>,
    localIds: Collection<StorageId>
  ): IdDifferenceResult {
    val remoteByRawId: Map<String, StorageId> = remoteIds.associateBy { encodeWithPadding(it.raw) }
    val localByRawId: Map<String, StorageId> = localIds.associateBy { encodeWithPadding(it.raw) }

    var hasTypeMismatch = remoteByRawId.size != remoteIds.size || localByRawId.size != localIds.size

    val remoteOnlyRawIds: MutableSet<String> = (remoteByRawId.keys - localByRawId.keys).toMutableSet()
    val localOnlyRawIds: MutableSet<String> = (localByRawId.keys - remoteByRawId.keys).toMutableSet()
    val sharedRawIds: Set<String> = localByRawId.keys.intersect(remoteByRawId.keys)

    for (rawId in sharedRawIds) {
      val remote = remoteByRawId[rawId]!!
      val local = localByRawId[rawId]!!

      if (remote.type != local.type) {
        remoteOnlyRawIds.remove(rawId)
        localOnlyRawIds.remove(rawId)
        hasTypeMismatch = true
        Log.w(TAG, "Remote type ${remote.type} did not match local type ${local.type}!")
      }
    }

    val remoteOnlyKeys = remoteOnlyRawIds.mapNotNull { remoteByRawId[it] }
    val localOnlyKeys = localOnlyRawIds.mapNotNull { localByRawId[it] }

    return IdDifferenceResult(remoteOnlyKeys, localOnlyKeys, hasTypeMismatch)
  }

  @JvmStatic
  fun generateKey(): ByteArray {
    return keyGenerator.generate()
  }

  @JvmStatic
  @VisibleForTesting
  fun setTestKeyGenerator(testKeyGenerator: StorageKeyGenerator?) {
    keyGenerator = testKeyGenerator ?: KEY_GENERATOR
  }

  @JvmStatic
  fun profileKeyChanged(update: StorageRecordUpdate<ZonaRosaContactRecord>): Boolean {
    return update.old.proto.profileKey != update.new.proto.profileKey
  }

  @JvmStatic
  fun buildAccountRecord(context: Context, self: Recipient): ZonaRosaStorageRecord {
    var self = self
    var selfRecord: RecipientRecord? = ZonaRosaDatabase.recipients.getRecordForSync(self.id)
    val pinned: List<RecipientRecord> = ZonaRosaDatabase.threads.getPinnedRecipientIds()
      .mapNotNull { ZonaRosaDatabase.recipients.getRecordForSync(it) }

    val storyViewReceiptsState = if (ZonaRosaStore.story.viewedReceiptsEnabled) {
      OptionalBool.ENABLED
    } else {
      OptionalBool.DISABLED
    }

    if (self.storageId == null || (selfRecord != null && selfRecord.storageId == null)) {
      Log.w(TAG, "[buildAccountRecord] No storageId for self or record! Generating. (Self: ${self.storageId != null}, Record: ${selfRecord?.storageId != null})")
      ZonaRosaDatabase.recipients.updateStorageId(self.id, generateKey())
      self = self().fresh()
      selfRecord = ZonaRosaDatabase.recipients.getRecordForSync(self.id)
    }

    if (selfRecord == null) {
      Log.w(TAG, "[buildAccountRecord] Could not find a RecipientRecord for ourselves! ID: ${self.id}")
    } else if (!selfRecord.storageId.contentEquals(self.storageId)) {
      Log.w(TAG, "[buildAccountRecord] StorageId on RecipientRecord did not match self! ID: ${self.id}")
    }

    val storageId = selfRecord?.storageId ?: self.storageId

    val accountRecord = ZonaRosaAccountRecord.newBuilder(selfRecord?.syncExtras?.storageProto).apply {
      profileKey = self.profileKey?.toByteString() ?: ByteString.EMPTY
      givenName = self.profileName.givenName
      familyName = self.profileName.familyName
      avatarUrlPath = self.profileAvatar ?: ""
      noteToSelfArchived = selfRecord != null && selfRecord.syncExtras.isArchived
      noteToSelfMarkedUnread = selfRecord != null && selfRecord.syncExtras.isForcedUnread
      typingIndicators = ZonaRosaPreferences.isTypingIndicatorsEnabled(context)
      readReceipts = ZonaRosaPreferences.isReadReceiptsEnabled(context)
      sealedSenderIndicators = ZonaRosaPreferences.isShowUnidentifiedDeliveryIndicatorsEnabled(context)
      linkPreviews = ZonaRosaStore.settings.isLinkPreviewsEnabled
      unlistedPhoneNumber = ZonaRosaStore.phoneNumberPrivacy.phoneNumberDiscoverabilityMode == PhoneNumberDiscoverabilityMode.NOT_DISCOVERABLE
      phoneNumberSharingMode = StorageSyncModels.localToRemotePhoneNumberSharingMode(ZonaRosaStore.phoneNumberPrivacy.phoneNumberSharingMode)
      pinnedConversations = StorageSyncModels.localToRemotePinnedConversations(pinned)
      preferContactAvatars = ZonaRosaStore.settings.isPreferSystemContactPhotos
      primarySendsSms = false
      universalExpireTimer = ZonaRosaStore.settings.universalExpireTimer
      preferredReactionEmoji = ZonaRosaStore.emoji.reactions
      displayBadgesOnProfile = ZonaRosaStore.inAppPayments.getDisplayBadgesOnProfile()
      subscriptionManuallyCancelled = isUserManuallyCancelled(InAppPaymentSubscriberRecord.Type.DONATION)
      keepMutedChatsArchived = ZonaRosaStore.settings.shouldKeepMutedChatsArchived()
      hasSetMyStoriesPrivacy = ZonaRosaStore.story.userHasBeenNotifiedAboutStories
      hasViewedOnboardingStory = ZonaRosaStore.story.userHasViewedOnboardingStory
      storiesDisabled = ZonaRosaStore.story.isFeatureDisabled
      storyViewReceiptsEnabled = storyViewReceiptsState
      hasSeenGroupStoryEducationSheet = ZonaRosaStore.story.userHasSeenGroupStoryEducationSheet
      hasCompletedUsernameOnboarding = ZonaRosaStore.uiHints.hasCompletedUsernameOnboarding()
      avatarColor = StorageSyncModels.localToRemoteAvatarColor(self.avatarColor)
      username = ZonaRosaStore.account.username ?: ""
      usernameLink = ZonaRosaStore.account.usernameLink?.let { linkComponents ->
        AccountRecord.UsernameLink(
          entropy = linkComponents.entropy.toByteString(),
          serverId = linkComponents.serverId.toByteArray().toByteString(),
          color = StorageSyncModels.localToRemoteUsernameColor(ZonaRosaStore.misc.usernameQrCodeColorScheme)
        )
      }

      hasBackup = ZonaRosaStore.backup.areBackupsEnabled && ZonaRosaStore.backup.hasBackupBeenUploaded
      backupTier = when {
        ZonaRosaStore.account.isLinkedDevice -> null
        ZonaRosaStore.backup.areBackupsEnabled && ZonaRosaStore.backup.backupTier != null -> getBackupLevelValue(ZonaRosaStore.backup.backupTier!!)
        ZonaRosaStore.backup.backupTierInternalOverride != null -> getBackupLevelValue(ZonaRosaStore.backup.backupTierInternalOverride!!)
        else -> null
      }

      notificationProfileManualOverride = getNotificationProfileManualOverride()

      getSubscriber(InAppPaymentSubscriberRecord.Type.DONATION)?.let {
        safeSetSubscriber(it.subscriberId.bytes.toByteString(), it.currency?.currencyCode ?: "")
      }

      getSubscriber(InAppPaymentSubscriberRecord.Type.BACKUP)?.let {
        safeSetBackupsSubscriber(it.subscriberId.bytes.toByteString(), it.iapSubscriptionId)
      }

      safeSetPayments(ZonaRosaStore.payments.mobileCoinPaymentsEnabled(), Optional.ofNullable(ZonaRosaStore.payments.paymentsEntropy).map { obj: Entropy -> obj.bytes }.orElse(null))
      automaticKeyVerificationDisabled = !ZonaRosaStore.settings.automaticVerificationEnabled
    }

    return accountRecord.toZonaRosaAccountRecord(StorageId.forAccount(storageId)).toZonaRosaStorageRecord()
  }

  // TODO: Currently we don't have access to the private values of the BackupLevel. Update when it becomes available.
  private fun getBackupLevelValue(tier: MessageBackupTier): Long {
    return when (tier) {
      MessageBackupTier.FREE -> 200
      MessageBackupTier.PAID -> 201
    }
  }

  private fun getNotificationProfileManualOverride(): AccountRecord.NotificationProfileManualOverride? {
    val profile = ZonaRosaDatabase.notificationProfiles.getProfile(ZonaRosaStore.notificationProfile.manuallyEnabledProfile)
    return if (profile != null && profile.deletedTimestampMs == 0L) {
      Log.i(TAG, "Setting a manually enabled profile ${profile.id}")
      // From [StorageService.proto], end timestamp should be unset if no timespan was chosen in the UI
      val endTimestamp = if (ZonaRosaStore.notificationProfile.manuallyEnabledUntil == Long.MAX_VALUE) 0 else ZonaRosaStore.notificationProfile.manuallyEnabledUntil
      AccountRecord.NotificationProfileManualOverride(
        enabled = AccountRecord.NotificationProfileManualOverride.ManuallyEnabled(
          id = UuidUtil.toByteArray(profile.notificationProfileId.uuid).toByteString(),
          endAtTimestampMs = endTimestamp
        )
      )
    } else if (ZonaRosaStore.notificationProfile.manuallyDisabledAt != 0L) {
      Log.i(TAG, "Setting a manually disabled profile ${ZonaRosaStore.notificationProfile.manuallyDisabledAt}")
      AccountRecord.NotificationProfileManualOverride(
        disabledAtTimestampMs = ZonaRosaStore.notificationProfile.manuallyDisabledAt
      )
    } else {
      null
    }
  }

  @JvmStatic
  fun applyAccountStorageSyncUpdates(context: Context, self: Recipient, updatedRecord: ZonaRosaAccountRecord, fetchProfile: Boolean) {
    val localRecord = buildAccountRecord(context, self).let { it.proto.account!!.toZonaRosaAccountRecord(it.id) }
    applyAccountStorageSyncUpdates(context, self, StorageRecordUpdate(localRecord, updatedRecord), fetchProfile)
  }

  @JvmStatic
  fun applyAccountStorageSyncUpdates(context: Context, self: Recipient, update: StorageRecordUpdate<ZonaRosaAccountRecord>, fetchProfile: Boolean) {
    ZonaRosaDatabase.recipients.applyStorageSyncAccountUpdate(update)

    ZonaRosaPreferences.setReadReceiptsEnabled(context, update.new.proto.readReceipts)
    ZonaRosaPreferences.setTypingIndicatorsEnabled(context, update.new.proto.typingIndicators)
    ZonaRosaPreferences.setShowUnidentifiedDeliveryIndicatorsEnabled(context, update.new.proto.sealedSenderIndicators)
    ZonaRosaStore.settings.isLinkPreviewsEnabled = update.new.proto.linkPreviews
    ZonaRosaStore.phoneNumberPrivacy.phoneNumberDiscoverabilityMode = if (update.new.proto.unlistedPhoneNumber) PhoneNumberDiscoverabilityMode.NOT_DISCOVERABLE else PhoneNumberDiscoverabilityMode.DISCOVERABLE
    ZonaRosaStore.phoneNumberPrivacy.phoneNumberSharingMode = StorageSyncModels.remoteToLocalPhoneNumberSharingMode(update.new.proto.phoneNumberSharingMode)
    ZonaRosaStore.settings.isPreferSystemContactPhotos = update.new.proto.preferContactAvatars
    ZonaRosaStore.payments.setEnabledAndEntropy(update.new.proto.payments?.enabled == true, Entropy.fromBytes(update.new.proto.payments?.entropy?.toByteArray()))
    ZonaRosaStore.settings.universalExpireTimer = update.new.proto.universalExpireTimer
    ZonaRosaStore.emoji.reactions = update.new.proto.preferredReactionEmoji
    ZonaRosaStore.inAppPayments.setDisplayBadgesOnProfile(update.new.proto.displayBadgesOnProfile)
    ZonaRosaStore.settings.setKeepMutedChatsArchived(update.new.proto.keepMutedChatsArchived)
    ZonaRosaStore.story.userHasBeenNotifiedAboutStories = update.new.proto.hasSetMyStoriesPrivacy
    ZonaRosaStore.story.userHasViewedOnboardingStory = update.new.proto.hasViewedOnboardingStory
    ZonaRosaStore.story.isFeatureDisabled = update.new.proto.storiesDisabled
    ZonaRosaStore.story.userHasSeenGroupStoryEducationSheet = update.new.proto.hasSeenGroupStoryEducationSheet
    ZonaRosaStore.uiHints.setHasCompletedUsernameOnboarding(update.new.proto.hasCompletedUsernameOnboarding)

    if (ZonaRosaStore.settings.automaticVerificationEnabled && update.new.proto.automaticKeyVerificationDisabled) {
      ZonaRosaDatabase.recipients.clearAllKeyTransparencyData()
    }
    ZonaRosaStore.settings.automaticVerificationEnabled = !update.new.proto.automaticKeyVerificationDisabled

    if (update.new.proto.storyViewReceiptsEnabled == OptionalBool.UNSET) {
      ZonaRosaStore.story.viewedReceiptsEnabled = update.new.proto.readReceipts
    } else {
      ZonaRosaStore.story.viewedReceiptsEnabled = update.new.proto.storyViewReceiptsEnabled == OptionalBool.ENABLED
    }

    val remoteSubscriber = StorageSyncModels.remoteToLocalDonorSubscriber(update.new.proto.subscriberId, update.new.proto.subscriberCurrencyCode)
    if (remoteSubscriber != null) {
      setSubscriber(remoteSubscriber)
    }

    val remoteBackupsSubscriber = StorageSyncModels.remoteToLocalBackupSubscriber(update.new.proto.backupSubscriberData)
    if (remoteBackupsSubscriber != null) {
      setSubscriber(remoteBackupsSubscriber)
    }

    if (update.new.proto.subscriptionManuallyCancelled && !update.old.proto.subscriptionManuallyCancelled) {
      ZonaRosaStore.inAppPayments.updateLocalStateForManualCancellation(InAppPaymentSubscriberRecord.Type.DONATION)
    }

    if (fetchProfile && update.new.proto.avatarUrlPath.isNotBlank()) {
      AppDependencies.jobManager.add(RetrieveProfileAvatarJob(self, update.new.proto.avatarUrlPath))
    }

    if (update.new.proto.username != update.old.proto.username) {
      ZonaRosaStore.account.username = update.new.proto.username
      ZonaRosaStore.account.usernameSyncState = AccountValues.UsernameSyncState.IN_SYNC
      ZonaRosaStore.account.usernameSyncErrorCount = 0
    }

    if (update.new.proto.usernameLink != null) {
      ZonaRosaStore.account.usernameLink = UsernameLinkComponents(
        update.new.proto.usernameLink!!.entropy.toByteArray(),
        UuidUtil.parseOrThrow(update.new.proto.usernameLink!!.serverId.toByteArray())
      )

      ZonaRosaStore.misc.usernameQrCodeColorScheme = StorageSyncModels.remoteToLocalUsernameColor(update.new.proto.usernameLink!!.color)
    }

    if (update.new.proto.notificationProfileManualOverride != null) {
      if (update.new.proto.notificationProfileManualOverride!!.enabled != null) {
        Log.i(TAG, "Found a remote enabled notification override")
        val remoteProfile = update.new.proto.notificationProfileManualOverride!!.enabled!!
        val remoteId = UuidUtil.parseOrNull(remoteProfile.id)
        val remoteEndTime = if (remoteProfile.endAtTimestampMs == 0L) Long.MAX_VALUE else remoteProfile.endAtTimestampMs

        if (remoteId == null) {
          Log.w(TAG, "Remote notification profile id is not valid")
        } else {
          val query = SqlUtil.buildQuery("${NotificationProfileTables.NotificationProfileTable.NOTIFICATION_PROFILE_ID} = ?", NotificationProfileId(remoteId))
          val localProfile = ZonaRosaDatabase.notificationProfiles.getProfile(query)

          if (localProfile == null) {
            Log.w(TAG, "Unable to find local notification profile with given remote id $remoteId")
          } else {
            Log.i(TAG, "Setting manually enabled profile to ${localProfile.id} ending at $remoteEndTime.")
            ZonaRosaStore.notificationProfile.manuallyEnabledProfile = localProfile.id
            ZonaRosaStore.notificationProfile.manuallyEnabledUntil = remoteEndTime
            ZonaRosaStore.notificationProfile.manuallyDisabledAt = 0L
          }
        }
      } else if (update.new.proto.notificationProfileManualOverride!!.disabledAtTimestampMs != null) {
        Log.i(TAG, "Found a remote disabled notification override for ${update.new.proto.notificationProfileManualOverride!!.disabledAtTimestampMs!!}")
        ZonaRosaStore.notificationProfile.manuallyEnabledProfile = 0
        ZonaRosaStore.notificationProfile.manuallyEnabledUntil = 0
        ZonaRosaStore.notificationProfile.manuallyDisabledAt = update.new.proto.notificationProfileManualOverride!!.disabledAtTimestampMs!!
      }
    }
  }

  @JvmStatic
  fun scheduleSyncForDataChange() {
    if (!ZonaRosaStore.registration.isRegistrationComplete) {
      Log.d(TAG, "Registration still ongoing. Ignore sync request.")
      return
    }
    AppDependencies.jobManager.add(StorageSyncJob.forLocalChange())
  }

  @JvmStatic
  fun scheduleRoutineSync() {
    val timeSinceLastSync = System.currentTimeMillis() - ZonaRosaStore.storageService.lastSyncTime

    if (timeSinceLastSync > REFRESH_INTERVAL && ZonaRosaStore.registration.isRegistrationComplete) {
      Log.d(TAG, "Scheduling a sync. Last sync was $timeSinceLastSync ms ago.")
      AppDependencies.jobManager.add(StorageSyncJob.forRemoteChange())
    } else {
      Log.d(TAG, "No need for sync. Last sync was $timeSinceLastSync ms ago.")
    }
  }

  class IdDifferenceResult(
    @JvmField val remoteOnlyIds: List<StorageId>,
    @JvmField val localOnlyIds: List<StorageId>,
    val hasTypeMismatches: Boolean
  ) {
    val isEmpty: Boolean
      get() = remoteOnlyIds.isEmpty() && localOnlyIds.isEmpty()

    override fun toString(): String {
      return "remoteOnly: ${remoteOnlyIds.size}, localOnly: ${localOnlyIds.size}, hasTypeMismatches: $hasTypeMismatches"
    }
  }

  class WriteOperationResult(
    @JvmField val manifest: ZonaRosaStorageManifest,
    @JvmField val inserts: List<ZonaRosaStorageRecord>,
    @JvmField val deletes: List<ByteArray>
  ) {
    val isEmpty: Boolean
      get() = inserts.isEmpty() && deletes.isEmpty()

    override fun toString(): String {
      return if (isEmpty) {
        "Empty"
      } else {
        "ManifestVersion: ${manifest.version}, Total Keys: ${manifest.storageIds.size}, Inserts: ${inserts.size}, Deletes: ${deletes.size}"
      }
    }
  }
}
