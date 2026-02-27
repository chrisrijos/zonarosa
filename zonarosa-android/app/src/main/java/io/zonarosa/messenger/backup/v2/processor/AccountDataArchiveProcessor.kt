/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.backup.v2.processor

import android.content.Context
import okio.ByteString.Companion.EMPTY
import okio.ByteString.Companion.toByteString
import io.zonarosa.core.util.UuidUtil
import io.zonarosa.core.util.logging.Log
import io.zonarosa.core.util.toByteArray
import io.zonarosa.libzonarosa.zkgroup.backups.BackupLevel
import io.zonarosa.messenger.attachments.AttachmentId
import io.zonarosa.messenger.backup.v2.ExportState
import io.zonarosa.messenger.backup.v2.ImportState
import io.zonarosa.messenger.backup.v2.MessageBackupTier
import io.zonarosa.messenger.backup.v2.database.restoreSelfFromBackup
import io.zonarosa.messenger.backup.v2.database.restoreWallpaperAttachment
import io.zonarosa.messenger.backup.v2.proto.AccountData
import io.zonarosa.messenger.backup.v2.proto.ChatStyle
import io.zonarosa.messenger.backup.v2.proto.Frame
import io.zonarosa.messenger.backup.v2.stream.BackupFrameEmitter
import io.zonarosa.messenger.backup.v2.util.ChatStyleConverter
import io.zonarosa.messenger.backup.v2.util.isValid
import io.zonarosa.messenger.backup.v2.util.isValidUsername
import io.zonarosa.messenger.backup.v2.util.parseChatWallpaper
import io.zonarosa.messenger.backup.v2.util.toLocal
import io.zonarosa.messenger.backup.v2.util.toLocalAttachment
import io.zonarosa.messenger.components.settings.app.subscription.InAppPaymentsRepository
import io.zonarosa.messenger.components.settings.app.usernamelinks.UsernameQrCodeColorScheme
import io.zonarosa.messenger.conversation.colors.ChatColors
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.database.model.InAppPaymentSubscriberRecord
import io.zonarosa.messenger.database.model.databaseprotos.InAppPaymentData
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobs.RetrieveProfileAvatarJob
import io.zonarosa.messenger.keyvalue.PhoneNumberPrivacyValues
import io.zonarosa.messenger.keyvalue.PhoneNumberPrivacyValues.PhoneNumberDiscoverabilityMode
import io.zonarosa.messenger.keyvalue.SettingsValues
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.recipients.RecipientId
import io.zonarosa.messenger.util.Environment
import io.zonarosa.messenger.util.ProfileUtil
import io.zonarosa.messenger.util.ZonaRosaPreferences
import io.zonarosa.messenger.webrtc.CallDataMode
import io.zonarosa.service.api.push.UsernameLinkComponents
import io.zonarosa.service.api.storage.IAPSubscriptionId.AppleIAPOriginalTransactionId
import io.zonarosa.service.api.storage.IAPSubscriptionId.GooglePlayBillingPurchaseToken
import io.zonarosa.service.api.subscriptions.SubscriberId
import java.util.Currency
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Handles importing/exporting [AccountData] frames for an archive.
 */
object AccountDataArchiveProcessor {

  private val TAG = Log.tag(AccountDataArchiveProcessor::class)

  fun export(db: ZonaRosaDatabase, zonarosaStore: ZonaRosaStore, exportState: ExportState, emitter: BackupFrameEmitter) {
    val context = AppDependencies.application

    val selfId = db.recipientTable.getByAci(zonarosaStore.accountValues.aci!!).get()
    val selfRecord = db.recipientTable.getRecordForSync(selfId)!!

    val donationCurrency = zonarosaStore.inAppPaymentValues.getRecurringDonationCurrency()
    val donationSubscriber = db.inAppPaymentSubscriberTable.getByCurrencyCode(donationCurrency.currencyCode)

    val chatColors = ZonaRosaStore.chatColors.chatColors
    val chatWallpaper = ZonaRosaStore.wallpaper.currentRawWallpaper

    val backupSubscriberRecord = db.inAppPaymentSubscriberTable.getBackupsSubscriber()

    val screenLockTimeoutSeconds = zonarosaStore.settingsValues.screenLockTimeout
    val screenLockTimeoutMinutes = if (screenLockTimeoutSeconds > 0) {
      screenLockTimeoutSeconds.seconds.inWholeMinutes.toInt()
    } else {
      null
    }

    val mobileAutoDownload = ZonaRosaPreferences.getMobileMediaDownloadAllowed(context)
    val wifiAutoDownload = ZonaRosaPreferences.getWifiMediaDownloadAllowed(context)

    val username = selfRecord.username?.takeIf { it.isValidUsername() }

    emitter.emit(
      Frame(
        account = AccountData(
          profileKey = selfRecord.profileKey?.toByteString() ?: EMPTY,
          givenName = selfRecord.zonarosaProfileName.givenName,
          familyName = selfRecord.zonarosaProfileName.familyName,
          avatarUrlPath = selfRecord.zonarosaProfileAvatar ?: "",
          svrPin = ZonaRosaStore.svr.pin ?: "",
          username = username,
          usernameLink = if (username != null && zonarosaStore.accountValues.usernameLink != null) {
            AccountData.UsernameLink(
              entropy = zonarosaStore.accountValues.usernameLink?.entropy?.toByteString() ?: EMPTY,
              serverId = zonarosaStore.accountValues.usernameLink?.serverId?.toByteArray()?.toByteString() ?: EMPTY,
              color = zonarosaStore.miscValues.usernameQrCodeColorScheme.toRemoteUsernameColor()
            )
          } else {
            null
          },
          accountSettings = AccountData.AccountSettings(
            storyViewReceiptsEnabled = zonarosaStore.storyValues.viewedReceiptsEnabled,
            typingIndicators = ZonaRosaPreferences.isTypingIndicatorsEnabled(context),
            readReceipts = ZonaRosaPreferences.isReadReceiptsEnabled(context),
            sealedSenderIndicators = ZonaRosaPreferences.isShowUnidentifiedDeliveryIndicatorsEnabled(context),
            allowSealedSenderFromAnyone = ZonaRosaPreferences.isUniversalUnidentifiedAccess(context),
            linkPreviews = zonarosaStore.settingsValues.isLinkPreviewsEnabled,
            notDiscoverableByPhoneNumber = zonarosaStore.phoneNumberPrivacyValues.phoneNumberDiscoverabilityMode == PhoneNumberDiscoverabilityMode.NOT_DISCOVERABLE,
            phoneNumberSharingMode = zonarosaStore.phoneNumberPrivacyValues.phoneNumberSharingMode.toRemotePhoneNumberSharingMode(),
            preferContactAvatars = zonarosaStore.settingsValues.isPreferSystemContactPhotos,
            universalExpireTimerSeconds = zonarosaStore.settingsValues.universalExpireTimer,
            preferredReactionEmoji = zonarosaStore.emojiValues.rawReactions,
            storiesDisabled = zonarosaStore.storyValues.isFeatureDisabled,
            hasViewedOnboardingStory = zonarosaStore.storyValues.userHasViewedOnboardingStory,
            hasSetMyStoriesPrivacy = zonarosaStore.storyValues.userHasBeenNotifiedAboutStories,
            keepMutedChatsArchived = zonarosaStore.settingsValues.shouldKeepMutedChatsArchived(),
            displayBadgesOnProfile = zonarosaStore.inAppPaymentValues.getDisplayBadgesOnProfile(),
            hasSeenGroupStoryEducationSheet = zonarosaStore.storyValues.userHasSeenGroupStoryEducationSheet,
            hasCompletedUsernameOnboarding = zonarosaStore.uiHintValues.hasCompletedUsernameOnboarding(),
            customChatColors = db.chatColorsTable.getSavedChatColors().toRemoteChatColors().also { colors -> exportState.customChatColorIds.addAll(colors.map { it.id }) },
            optimizeOnDeviceStorage = zonarosaStore.backupValues.optimizeStorage,
            backupTier = zonarosaStore.backupValues.backupTier.toRemoteBackupTier(),
            defaultSentMediaQuality = zonarosaStore.settingsValues.sentMediaQuality.toRemoteSentMediaQuality(),
            autoDownloadSettings = AccountData.AutoDownloadSettings(
              images = getRemoteAutoDownloadOption("image", mobileAutoDownload, wifiAutoDownload),
              audio = getRemoteAutoDownloadOption("audio", mobileAutoDownload, wifiAutoDownload),
              video = getRemoteAutoDownloadOption("video", mobileAutoDownload, wifiAutoDownload),
              documents = getRemoteAutoDownloadOption("documents", mobileAutoDownload, wifiAutoDownload)
            ),
            screenLockTimeoutMinutes = screenLockTimeoutMinutes,
            pinReminders = zonarosaStore.pinValues.arePinRemindersEnabled(),
            appTheme = zonarosaStore.settingsValues.theme.toRemoteAppTheme(),
            callsUseLessDataSetting = zonarosaStore.settingsValues.callDataMode.toRemoteCallsUseLessDataSetting(),
            defaultChatStyle = ChatStyleConverter.constructRemoteChatStyle(
              db = db,
              chatColors = chatColors,
              chatColorId = chatColors?.id?.takeIf { it.isValid(exportState) } ?: ChatColors.Id.NotSet,
              chatWallpaper = chatWallpaper,
              backupMode = exportState.backupMode
            ),
            allowAutomaticKeyVerification = zonarosaStore.settingsValues.automaticVerificationEnabled
          ),
          donationSubscriberData = donationSubscriber?.toSubscriberData(zonarosaStore.inAppPaymentValues.isDonationSubscriptionManuallyCancelled()),
          backupsSubscriberData = backupSubscriberRecord?.toIAPSubscriberData(),
          androidSpecificSettings = AccountData.AndroidSpecificSettings(
            useSystemEmoji = zonarosaStore.settingsValues.isPreferSystemEmoji,
            screenshotSecurity = ZonaRosaPreferences.isScreenSecurityEnabled(context),
            navigationBarSize = zonarosaStore.settingsValues.useCompactNavigationBar.toRemoteNavigationBarSize()
          ).takeUnless { Environment.IS_INSTRUMENTATION && ZonaRosaStore.backup.importedEmptyAndroidSettings },
          bioText = selfRecord.about ?: "",
          bioEmoji = selfRecord.aboutEmoji ?: "",
          keyTransparencyData = selfRecord.keyTransparencyData?.toByteString()
        )
      )
    )
  }

  fun import(accountData: AccountData, selfId: RecipientId, importState: ImportState) {
    ZonaRosaDatabase.recipients.restoreSelfFromBackup(accountData, selfId)

    ZonaRosaStore.account.setRegistered(true)
    if (accountData.svrPin.isNotBlank()) {
      ZonaRosaStore.svr.setPin(accountData.svrPin)
    }

    val context = AppDependencies.application
    val settings = accountData.accountSettings

    if (settings != null) {
      importSettings(context, settings, importState)
    }

    if (accountData.androidSpecificSettings != null) {
      ZonaRosaStore.settings.isPreferSystemEmoji = accountData.androidSpecificSettings.useSystemEmoji
      ZonaRosaPreferences.setScreenSecurityEnabled(context, accountData.androidSpecificSettings.screenshotSecurity)
      ZonaRosaStore.settings.useCompactNavigationBar = accountData.androidSpecificSettings.navigationBarSize.toLocalNavigationBarSize()
    } else if (Environment.IS_INSTRUMENTATION) {
      ZonaRosaStore.backup.importedEmptyAndroidSettings = true
    }

    if (accountData.bioText.isNotBlank() || accountData.bioEmoji.isNotBlank()) {
      ZonaRosaDatabase.recipients.setAbout(selfId, accountData.bioText.takeIf { it.isNotBlank() }, accountData.bioEmoji.takeIf { it.isNotBlank() })
    }

    if (accountData.donationSubscriberData != null) {
      if (accountData.donationSubscriberData.subscriberId.size > 0) {
        val remoteSubscriberId = SubscriberId.fromBytes(accountData.donationSubscriberData.subscriberId.toByteArray())
        val localSubscriber = InAppPaymentsRepository.getSubscriber(InAppPaymentSubscriberRecord.Type.DONATION)

        val subscriber = InAppPaymentSubscriberRecord(
          subscriberId = remoteSubscriberId,
          currency = Currency.getInstance(accountData.donationSubscriberData.currencyCode),
          type = InAppPaymentSubscriberRecord.Type.DONATION,
          requiresCancel = localSubscriber?.requiresCancel ?: accountData.donationSubscriberData.manuallyCancelled,
          paymentMethodType = InAppPaymentsRepository.getLatestPaymentMethodType(InAppPaymentSubscriberRecord.Type.DONATION),
          iapSubscriptionId = null
        )

        InAppPaymentsRepository.setSubscriber(subscriber)
      }

      if (accountData.donationSubscriberData.manuallyCancelled) {
        ZonaRosaStore.inAppPayments.updateLocalStateForManualCancellation(InAppPaymentSubscriberRecord.Type.DONATION)
      }
    }

    if (accountData.backupsSubscriberData != null && accountData.backupsSubscriberData.subscriberId.size > 0 && (accountData.backupsSubscriberData.purchaseToken != null || accountData.backupsSubscriberData.originalTransactionId != null)) {
      val remoteSubscriberId = SubscriberId.fromBytes(accountData.backupsSubscriberData.subscriberId.toByteArray())
      val localSubscriber = InAppPaymentsRepository.getSubscriber(InAppPaymentSubscriberRecord.Type.BACKUP)

      val subscriber = InAppPaymentSubscriberRecord(
        subscriberId = remoteSubscriberId,
        currency = localSubscriber?.currency,
        type = InAppPaymentSubscriberRecord.Type.BACKUP,
        requiresCancel = localSubscriber?.requiresCancel ?: false,
        paymentMethodType = InAppPaymentData.PaymentMethodType.UNKNOWN,
        iapSubscriptionId = if (accountData.backupsSubscriberData.purchaseToken != null) {
          GooglePlayBillingPurchaseToken(accountData.backupsSubscriberData.purchaseToken)
        } else {
          AppleIAPOriginalTransactionId(accountData.backupsSubscriberData.originalTransactionId!!)
        }
      )

      InAppPaymentsRepository.setSubscriber(subscriber)
    }

    if (accountData.avatarUrlPath.isNotEmpty()) {
      AppDependencies.jobManager.add(RetrieveProfileAvatarJob(Recipient.self().fresh(), accountData.avatarUrlPath))
    }

    if (accountData.usernameLink != null) {
      ZonaRosaStore.account.usernameLink = UsernameLinkComponents(
        accountData.usernameLink.entropy.toByteArray(),
        UuidUtil.parseOrThrow(accountData.usernameLink.serverId.toByteArray())
      )
      ZonaRosaStore.misc.usernameQrCodeColorScheme = accountData.usernameLink.color.toLocalUsernameColor()
    } else {
      ZonaRosaStore.account.usernameLink = null
    }

    ZonaRosaDatabase.recipients.setKeyTransparencyData(Recipient.self().aci.get(), accountData.keyTransparencyData?.toByteArray())

    ZonaRosaDatabase.runPostSuccessfulTransaction { ProfileUtil.handleSelfProfileKeyChange() }

    Recipient.self().live().refresh()
  }

  private fun importSettings(context: Context, settings: AccountData.AccountSettings, importState: ImportState) {
    ZonaRosaPreferences.setReadReceiptsEnabled(context, settings.readReceipts)
    ZonaRosaPreferences.setTypingIndicatorsEnabled(context, settings.typingIndicators)
    ZonaRosaPreferences.setShowUnidentifiedDeliveryIndicatorsEnabled(context, settings.sealedSenderIndicators)
    ZonaRosaPreferences.setIsUniversalUnidentifiedAccess(context, settings.allowSealedSenderFromAnyone)
    ZonaRosaStore.settings.isLinkPreviewsEnabled = settings.linkPreviews
    ZonaRosaStore.phoneNumberPrivacy.phoneNumberDiscoverabilityMode = if (settings.notDiscoverableByPhoneNumber) PhoneNumberDiscoverabilityMode.NOT_DISCOVERABLE else PhoneNumberDiscoverabilityMode.DISCOVERABLE
    ZonaRosaStore.phoneNumberPrivacy.phoneNumberSharingMode = settings.phoneNumberSharingMode.toLocalPhoneNumberMode()
    ZonaRosaStore.settings.isPreferSystemContactPhotos = settings.preferContactAvatars
    ZonaRosaStore.settings.universalExpireTimer = settings.universalExpireTimerSeconds
    ZonaRosaStore.emoji.reactions = settings.preferredReactionEmoji
    ZonaRosaStore.inAppPayments.setDisplayBadgesOnProfile(settings.displayBadgesOnProfile)
    ZonaRosaStore.settings.setKeepMutedChatsArchived(settings.keepMutedChatsArchived)
    ZonaRosaStore.story.userHasBeenNotifiedAboutStories = settings.hasSetMyStoriesPrivacy
    ZonaRosaStore.story.userHasViewedOnboardingStory = settings.hasViewedOnboardingStory
    ZonaRosaStore.story.isFeatureDisabled = settings.storiesDisabled
    ZonaRosaStore.story.userHasSeenGroupStoryEducationSheet = settings.hasSeenGroupStoryEducationSheet
    ZonaRosaStore.story.viewedReceiptsEnabled = settings.storyViewReceiptsEnabled ?: settings.readReceipts
    ZonaRosaStore.backup.optimizeStorage = settings.optimizeOnDeviceStorage
    ZonaRosaStore.backup.backupTier = settings.backupTier?.toLocalBackupTier()
    ZonaRosaStore.settings.sentMediaQuality = settings.defaultSentMediaQuality.toLocalSentMediaQuality()
    ZonaRosaStore.settings.setTheme(settings.appTheme.toLocalTheme())
    ZonaRosaStore.settings.setCallDataMode(settings.callsUseLessDataSetting.toLocalCallDataMode())
    ZonaRosaStore.settings.automaticVerificationEnabled = settings.allowAutomaticKeyVerification

    if (settings.autoDownloadSettings != null) {
      val mobileAndWifiDownloadSet = settings.autoDownloadSettings.toLocalAutoDownloadSet(AccountData.AutoDownloadSettings.AutoDownloadOption.WIFI_AND_CELLULAR)
      val wifiDownloadSet = mobileAndWifiDownloadSet + settings.autoDownloadSettings.toLocalAutoDownloadSet(AccountData.AutoDownloadSettings.AutoDownloadOption.WIFI)

      ZonaRosaPreferences.getSharedPreferences(context).edit().apply {
        putStringSet(ZonaRosaPreferences.MEDIA_DOWNLOAD_MOBILE_PREF, mobileAndWifiDownloadSet)
        putStringSet(ZonaRosaPreferences.MEDIA_DOWNLOAD_WIFI_PREF, wifiDownloadSet)
        apply()
      }
    }

    if (settings.screenLockTimeoutMinutes != null) {
      ZonaRosaStore.settings.screenLockTimeout = settings.screenLockTimeoutMinutes.minutes.inWholeSeconds
    }

    if (settings.pinReminders != null) {
      ZonaRosaStore.pin.setPinRemindersEnabled(settings.pinReminders)
    }

    settings.customChatColors
      .mapNotNull { chatColor ->
        val id = ChatColors.Id.forLongValue(chatColor.id)
        when {
          chatColor.solid != null -> {
            ChatColors.forColor(id, chatColor.solid)
          }
          chatColor.gradient != null -> {
            ChatColors.forGradient(
              id,
              ChatColors.LinearGradient(
                degrees = chatColor.gradient.angle.toFloat(),
                colors = chatColor.gradient.colors.toIntArray(),
                positions = chatColor.gradient.positions.toFloatArray()
              )
            )
          }
          else -> null
        }
      }
      .forEach { chatColor ->
        // We need to use the "NotSet" chatId so that this operation is treated as an insert rather than an update
        val saved = ZonaRosaDatabase.chatColors.saveChatColors(chatColor.withId(ChatColors.Id.NotSet))
        importState.remoteToLocalColorId[chatColor.id.longValue] = saved.id.longValue
      }

    if (settings.defaultChatStyle != null) {
      val chatColors = settings.defaultChatStyle.toLocal(importState)
      ZonaRosaStore.chatColors.chatColors = chatColors

      val wallpaperAttachmentId: AttachmentId? = settings.defaultChatStyle.wallpaperPhoto?.let { filePointer ->
        filePointer.toLocalAttachment()?.let {
          ZonaRosaDatabase.attachments.restoreWallpaperAttachment(it)
        }
      }

      ZonaRosaStore.wallpaper.wallpaper = settings.defaultChatStyle.parseChatWallpaper(wallpaperAttachmentId)
    } else {
      ZonaRosaStore.chatColors.chatColors = null
      ZonaRosaStore.wallpaper.wallpaper = null
    }

    if (settings.preferredReactionEmoji.isNotEmpty()) {
      ZonaRosaStore.emoji.reactions = settings.preferredReactionEmoji
    }

    if (settings.hasCompletedUsernameOnboarding) {
      ZonaRosaStore.uiHints.setHasCompletedUsernameOnboarding(true)
    }
  }

  private fun PhoneNumberPrivacyValues.PhoneNumberSharingMode.toRemotePhoneNumberSharingMode(): AccountData.PhoneNumberSharingMode {
    return when (this) {
      PhoneNumberPrivacyValues.PhoneNumberSharingMode.DEFAULT -> AccountData.PhoneNumberSharingMode.EVERYBODY
      PhoneNumberPrivacyValues.PhoneNumberSharingMode.EVERYBODY -> AccountData.PhoneNumberSharingMode.EVERYBODY
      PhoneNumberPrivacyValues.PhoneNumberSharingMode.NOBODY -> AccountData.PhoneNumberSharingMode.NOBODY
    }
  }

  private fun AccountData.PhoneNumberSharingMode.toLocalPhoneNumberMode(): PhoneNumberPrivacyValues.PhoneNumberSharingMode {
    return when (this) {
      AccountData.PhoneNumberSharingMode.UNKNOWN -> PhoneNumberPrivacyValues.PhoneNumberSharingMode.EVERYBODY
      AccountData.PhoneNumberSharingMode.EVERYBODY -> PhoneNumberPrivacyValues.PhoneNumberSharingMode.EVERYBODY
      AccountData.PhoneNumberSharingMode.NOBODY -> PhoneNumberPrivacyValues.PhoneNumberSharingMode.NOBODY
    }
  }

  private fun AccountData.UsernameLink.Color?.toLocalUsernameColor(): UsernameQrCodeColorScheme {
    return when (this) {
      AccountData.UsernameLink.Color.BLUE -> UsernameQrCodeColorScheme.Blue
      AccountData.UsernameLink.Color.WHITE -> UsernameQrCodeColorScheme.White
      AccountData.UsernameLink.Color.GREY -> UsernameQrCodeColorScheme.Grey
      AccountData.UsernameLink.Color.OLIVE -> UsernameQrCodeColorScheme.Tan
      AccountData.UsernameLink.Color.GREEN -> UsernameQrCodeColorScheme.Green
      AccountData.UsernameLink.Color.ORANGE -> UsernameQrCodeColorScheme.Orange
      AccountData.UsernameLink.Color.PINK -> UsernameQrCodeColorScheme.Pink
      AccountData.UsernameLink.Color.PURPLE -> UsernameQrCodeColorScheme.Purple
      else -> UsernameQrCodeColorScheme.Blue
    }
  }

  private fun UsernameQrCodeColorScheme.toRemoteUsernameColor(): AccountData.UsernameLink.Color {
    return when (this) {
      UsernameQrCodeColorScheme.Blue -> AccountData.UsernameLink.Color.BLUE
      UsernameQrCodeColorScheme.White -> AccountData.UsernameLink.Color.WHITE
      UsernameQrCodeColorScheme.Grey -> AccountData.UsernameLink.Color.GREY
      UsernameQrCodeColorScheme.Tan -> AccountData.UsernameLink.Color.OLIVE
      UsernameQrCodeColorScheme.Green -> AccountData.UsernameLink.Color.GREEN
      UsernameQrCodeColorScheme.Orange -> AccountData.UsernameLink.Color.ORANGE
      UsernameQrCodeColorScheme.Pink -> AccountData.UsernameLink.Color.PINK
      UsernameQrCodeColorScheme.Purple -> AccountData.UsernameLink.Color.PURPLE
    }
  }

  /**
   * This method only supports donations subscriber data, and assumes there is a currency code available.
   */
  private fun InAppPaymentSubscriberRecord.toSubscriberData(manuallyCancelled: Boolean): AccountData.SubscriberData {
    val subscriberId = subscriberId.bytes.toByteString()
    val currencyCode = currency!!.currencyCode
    return AccountData.SubscriberData(subscriberId = subscriberId, currencyCode = currencyCode, manuallyCancelled = manuallyCancelled)
  }

  private fun InAppPaymentSubscriberRecord?.toIAPSubscriberData(): AccountData.IAPSubscriberData? {
    if (this == null) {
      return null
    }

    val builder = AccountData.IAPSubscriberData.Builder()
      .subscriberId(this.subscriberId.bytes.toByteString())

    if (this.iapSubscriptionId?.purchaseToken != null) {
      builder.purchaseToken(this.iapSubscriptionId.purchaseToken)
    } else if (this.iapSubscriptionId?.originalTransactionId != null) {
      builder.originalTransactionId(this.iapSubscriptionId.originalTransactionId)
    }

    return builder.build()
  }

  private fun List<ChatColors>.toRemoteChatColors(): List<ChatStyle.CustomChatColor> {
    return this
      .mapNotNull { local ->
        if (local.linearGradient != null) {
          ChatStyle.CustomChatColor(
            id = local.id.longValue,
            gradient = ChatStyle.Gradient(
              angle = local.linearGradient.degrees.toInt(),
              colors = local.linearGradient.colors.toList(),
              positions = local.linearGradient.positions.toList()
            )
          )
        } else if (local.singleColor != null) {
          ChatStyle.CustomChatColor(
            id = local.id.longValue,
            solid = local.singleColor
          )
        } else {
          Log.w(TAG, "Invalid custom color (id = ${local.id}, no gradient or solid color!")
          null
        }
      }
  }

  private fun MessageBackupTier?.toRemoteBackupTier(): Long? {
    return when (this) {
      MessageBackupTier.FREE -> BackupLevel.FREE.value.toLong()
      MessageBackupTier.PAID -> BackupLevel.PAID.value.toLong()
      null -> null
    }
  }

  private fun Long?.toLocalBackupTier(): MessageBackupTier? {
    return when (this) {
      BackupLevel.FREE.value.toLong() -> MessageBackupTier.FREE
      BackupLevel.PAID.value.toLong() -> MessageBackupTier.PAID
      else -> null
    }
  }

  private fun io.zonarosa.messenger.mms.SentMediaQuality.toRemoteSentMediaQuality(): AccountData.SentMediaQuality {
    return when (this) {
      io.zonarosa.messenger.mms.SentMediaQuality.STANDARD -> AccountData.SentMediaQuality.STANDARD
      io.zonarosa.messenger.mms.SentMediaQuality.HIGH -> AccountData.SentMediaQuality.HIGH
    }
  }

  private fun AccountData.SentMediaQuality?.toLocalSentMediaQuality(): io.zonarosa.messenger.mms.SentMediaQuality {
    return when (this) {
      AccountData.SentMediaQuality.HIGH -> io.zonarosa.messenger.mms.SentMediaQuality.HIGH
      AccountData.SentMediaQuality.STANDARD -> io.zonarosa.messenger.mms.SentMediaQuality.STANDARD
      AccountData.SentMediaQuality.UNKNOWN_QUALITY -> io.zonarosa.messenger.mms.SentMediaQuality.STANDARD
      null -> io.zonarosa.messenger.mms.SentMediaQuality.STANDARD
    }
  }

  private fun getRemoteAutoDownloadOption(mediaType: String, mobileSet: Set<String>, wifiSet: Set<String>): AccountData.AutoDownloadSettings.AutoDownloadOption {
    return when {
      mobileSet.contains(mediaType) -> AccountData.AutoDownloadSettings.AutoDownloadOption.WIFI_AND_CELLULAR
      wifiSet.contains(mediaType) -> AccountData.AutoDownloadSettings.AutoDownloadOption.WIFI
      else -> AccountData.AutoDownloadSettings.AutoDownloadOption.NEVER
    }
  }

  private fun AccountData.AutoDownloadSettings.toLocalAutoDownloadSet(option: AccountData.AutoDownloadSettings.AutoDownloadOption): Set<String> {
    val out = mutableSetOf<String>()
    if (this.images == option) {
      out += "image"
    }
    if (this.audio == option) {
      out += "audio"
    }
    if (this.video == option) {
      out += "video"
    }
    if (this.documents == option) {
      out += "documents"
    }
    return out
  }

  private fun Boolean.toRemoteNavigationBarSize(): AccountData.AndroidSpecificSettings.NavigationBarSize {
    return if (this) {
      AccountData.AndroidSpecificSettings.NavigationBarSize.COMPACT
    } else {
      AccountData.AndroidSpecificSettings.NavigationBarSize.NORMAL
    }
  }

  private fun AccountData.AndroidSpecificSettings.NavigationBarSize.toLocalNavigationBarSize(): Boolean {
    return when (this) {
      AccountData.AndroidSpecificSettings.NavigationBarSize.COMPACT -> true
      AccountData.AndroidSpecificSettings.NavigationBarSize.NORMAL -> false
      AccountData.AndroidSpecificSettings.NavigationBarSize.UNKNOWN_BAR_SIZE -> false
    }
  }

  private fun SettingsValues.Theme.toRemoteAppTheme(): AccountData.AppTheme {
    return when (this) {
      SettingsValues.Theme.SYSTEM -> AccountData.AppTheme.SYSTEM
      SettingsValues.Theme.LIGHT -> AccountData.AppTheme.LIGHT
      SettingsValues.Theme.DARK -> AccountData.AppTheme.DARK
    }
  }

  private fun AccountData.AppTheme.toLocalTheme(): SettingsValues.Theme {
    return when (this) {
      AccountData.AppTheme.SYSTEM -> SettingsValues.Theme.SYSTEM
      AccountData.AppTheme.LIGHT -> SettingsValues.Theme.LIGHT
      AccountData.AppTheme.DARK -> SettingsValues.Theme.DARK
      AccountData.AppTheme.UNKNOWN_APP_THEME -> SettingsValues.Theme.SYSTEM
    }
  }

  private fun CallDataMode.toRemoteCallsUseLessDataSetting(): AccountData.CallsUseLessDataSetting {
    return when (this) {
      CallDataMode.LOW_ALWAYS -> AccountData.CallsUseLessDataSetting.WIFI_AND_MOBILE_DATA
      CallDataMode.HIGH_ON_WIFI -> AccountData.CallsUseLessDataSetting.MOBILE_DATA_ONLY
      CallDataMode.HIGH_ALWAYS -> AccountData.CallsUseLessDataSetting.NEVER
    }
  }

  private fun AccountData.CallsUseLessDataSetting.toLocalCallDataMode(): CallDataMode {
    return when (this) {
      AccountData.CallsUseLessDataSetting.WIFI_AND_MOBILE_DATA -> CallDataMode.LOW_ALWAYS
      AccountData.CallsUseLessDataSetting.MOBILE_DATA_ONLY -> CallDataMode.HIGH_ON_WIFI
      AccountData.CallsUseLessDataSetting.NEVER -> CallDataMode.HIGH_ALWAYS
      AccountData.CallsUseLessDataSetting.UNKNOWN_CALL_DATA_SETTING -> CallDataMode.HIGH_ALWAYS
    }
  }
}
