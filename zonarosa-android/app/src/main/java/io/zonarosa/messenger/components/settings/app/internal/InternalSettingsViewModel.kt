package io.zonarosa.messenger.components.settings.app.internal

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.reactivex.rxjava3.core.Observable
import io.zonarosa.ringrtc.CallManager
import io.zonarosa.messenger.database.model.RemoteMegaphoneRecord
import io.zonarosa.messenger.jobs.StoryOnboardingDownloadJob
import io.zonarosa.messenger.keyvalue.InternalValues
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.stories.Stories
import io.zonarosa.messenger.util.livedata.Store

class InternalSettingsViewModel(private val repository: InternalSettingsRepository) : ViewModel() {
  private val preferenceDataStore = ZonaRosaStore.getPreferenceDataStore()

  private val store = Store(getState())

  init {
    repository.getEmojiVersionInfo { version ->
      store.update { it.copy(emojiVersion = version) }
    }

    val pendingOneTimeDonation: Observable<Boolean> = ZonaRosaStore.inAppPayments.observablePendingOneTimeDonation
      .distinctUntilChanged()
      .map { it.isPresent }

    store.update(pendingOneTimeDonation) { pending, state ->
      state.copy(hasPendingOneTimeDonation = pending)
    }
  }

  val state: LiveData<InternalSettingsState> = store.stateLiveData

  fun setSeeMoreUserDetails(enabled: Boolean) {
    preferenceDataStore.putBoolean(InternalValues.RECIPIENT_DETAILS, enabled)
    refresh()
  }

  fun setShakeToReport(enabled: Boolean) {
    preferenceDataStore.putBoolean(InternalValues.SHAKE_TO_REPORT, enabled)
    refresh()
  }

  fun setShowMediaArchiveStateHint(enabled: Boolean) {
    preferenceDataStore.putBoolean(InternalValues.SHOW_ARCHIVE_STATE_HINT, enabled)
    refresh()
  }

  fun setDisableStorageService(enabled: Boolean) {
    preferenceDataStore.putBoolean(InternalValues.DISABLE_STORAGE_SERVICE, enabled)
    refresh()
  }

  fun setGv2ForceInvites(enabled: Boolean) {
    preferenceDataStore.putBoolean(InternalValues.GV2_FORCE_INVITES, enabled)
    refresh()
  }

  fun setGv2IgnoreP2PChanges(enabled: Boolean) {
    preferenceDataStore.putBoolean(InternalValues.GV2_IGNORE_P2P_CHANGES, enabled)
    refresh()
  }

  fun setAllowCensorshipSetting(enabled: Boolean) {
    preferenceDataStore.putBoolean(InternalValues.ALLOW_CENSORSHIP_SETTING, enabled)
    refresh()
  }

  fun setForceWebsocketMode(enabled: Boolean) {
    preferenceDataStore.putBoolean(InternalValues.FORCE_WEBSOCKET_MODE, enabled)
    refresh()
  }

  fun resetPnpInitializedState() {
    ZonaRosaStore.misc.hasPniInitializedDevices = false
    refresh()
  }

  fun setUseBuiltInEmoji(enabled: Boolean) {
    preferenceDataStore.putBoolean(InternalValues.FORCE_BUILT_IN_EMOJI, enabled)
    refresh()
  }

  fun setRemoveSenderKeyMinimum(enabled: Boolean) {
    preferenceDataStore.putBoolean(InternalValues.REMOVE_SENDER_KEY_MINIMUM, enabled)
    refresh()
  }

  fun setDelayResends(enabled: Boolean) {
    preferenceDataStore.putBoolean(InternalValues.DELAY_RESENDS, enabled)
    refresh()
  }

  fun setInternalGroupCallingServer(server: String?) {
    preferenceDataStore.putString(InternalValues.CALLING_SERVER, server)
    refresh()
  }

  fun setInternalCallingDataMode(dataMode: CallManager.DataMode) {
    preferenceDataStore.putInt(InternalValues.CALLING_DATA_MODE, dataMode.ordinal)
    refresh()
  }

  fun setInternalCallingDisableTelecom(enabled: Boolean) {
    preferenceDataStore.putBoolean(InternalValues.CALLING_DISABLE_TELECOM, enabled)
    refresh()
  }

  fun setInternalCallingSetAudioConfig(enabled: Boolean) {
    preferenceDataStore.putBoolean(InternalValues.CALLING_SET_AUDIO_CONFIG, enabled)
    refresh()
  }

  fun setInternalCallingUseOboeAdm(enabled: Boolean) {
    preferenceDataStore.putBoolean(InternalValues.CALLING_USE_OBOE_ADM, enabled)
    refresh()
  }

  fun setInternalCallingUseSoftwareAec(enabled: Boolean) {
    preferenceDataStore.putBoolean(InternalValues.CALLING_USE_SOFTWARE_AEC, enabled)
    refresh()
  }

  fun setInternalCallingUseSoftwareNs(enabled: Boolean) {
    preferenceDataStore.putBoolean(InternalValues.CALLING_USE_SOFTWARE_NS, enabled)
    refresh()
  }

  fun setInternalCallingUseInputLowLatency(enabled: Boolean) {
    preferenceDataStore.putBoolean(InternalValues.CALLING_USE_INPUT_LOW_LATENCY, enabled)
    refresh()
  }

  fun setInternalCallingUseInputVoiceComm(enabled: Boolean) {
    preferenceDataStore.putBoolean(InternalValues.CALLING_USE_INPUT_VOICE_COMM, enabled)
    refresh()
  }

  fun setUseConversationItemV2Media(enabled: Boolean) {
    ZonaRosaStore.internal.useConversationItemV2Media = enabled
    refresh()
  }

  fun setUseNewMediaActivity(enabled: Boolean) {
    ZonaRosaStore.internal.useNewMediaActivity = enabled
    refresh()
  }

  fun setHevcEncoding(enabled: Boolean) {
    ZonaRosaStore.internal.hevcEncoding = enabled
    refresh()
  }

  fun addSampleReleaseNote(callToAction: String = "action") {
    repository.addSampleReleaseNote(callToAction)
  }

  fun addRemoteDonateMegaphone() {
    repository.addRemoteMegaphone(RemoteMegaphoneRecord.ActionId.DONATE)
  }

  fun addRemoteDonateFriendMegaphone() {
    repository.addRemoteMegaphone(RemoteMegaphoneRecord.ActionId.DONATE_FOR_FRIEND)
  }

  fun enqueueSubscriptionRedemption() {
    repository.enqueueSubscriptionRedemption()
  }

  fun refresh() {
    store.update { getState().copy(emojiVersion = it.emojiVersion) }
  }

  private fun getState() = InternalSettingsState(
    seeMoreUserDetails = ZonaRosaStore.internal.recipientDetails,
    shakeToReport = ZonaRosaStore.internal.shakeToReport,
    showArchiveStateHint = ZonaRosaStore.internal.showArchiveStateHint,
    gv2forceInvites = ZonaRosaStore.internal.gv2ForceInvites,
    gv2ignoreP2PChanges = ZonaRosaStore.internal.gv2IgnoreP2PChanges,
    allowCensorshipSetting = ZonaRosaStore.internal.allowChangingCensorshipSetting,
    forceWebsocketMode = ZonaRosaStore.internal.isWebsocketModeForced,
    callingServer = ZonaRosaStore.internal.groupCallingServer,
    callingDataMode = ZonaRosaStore.internal.callingDataMode,
    callingDisableTelecom = ZonaRosaStore.internal.callingDisableTelecom,
    callingSetAudioConfig = ZonaRosaStore.internal.callingSetAudioConfig,
    callingUseOboeAdm = ZonaRosaStore.internal.callingUseOboeAdm,
    callingUseSoftwareAec = ZonaRosaStore.internal.callingUseSoftwareAec,
    callingUseSoftwareNs = ZonaRosaStore.internal.callingUseSoftwareNs,
    callingUseInputLowLatency = ZonaRosaStore.internal.callingUseInputLowLatency,
    callingUseInputVoiceComm = ZonaRosaStore.internal.callingUseInputVoiceComm,
    useBuiltInEmojiSet = ZonaRosaStore.internal.forceBuiltInEmoji,
    emojiVersion = null,
    removeSenderKeyMinimium = ZonaRosaStore.internal.removeSenderKeyMinimum,
    delayResends = ZonaRosaStore.internal.delayResends,
    disableStorageService = ZonaRosaStore.internal.storageServiceDisabled,
    canClearOnboardingState = ZonaRosaStore.story.hasDownloadedOnboardingStory && Stories.isFeatureEnabled(),
    pnpInitialized = ZonaRosaStore.misc.hasPniInitializedDevices,
    useConversationItemV2ForMedia = ZonaRosaStore.internal.useConversationItemV2Media,
    hasPendingOneTimeDonation = ZonaRosaStore.inAppPayments.getPendingOneTimeDonation() != null,
    hevcEncoding = ZonaRosaStore.internal.hevcEncoding,
    forceSplitPane = ZonaRosaStore.internal.forceSplitPane,
    useNewMediaActivity = ZonaRosaStore.internal.useNewMediaActivity
  )

  fun onClearOnboardingState() {
    ZonaRosaStore.story.hasDownloadedOnboardingStory = false
    ZonaRosaStore.story.userHasViewedOnboardingStory = false
    Stories.onStorySettingsChanged(Recipient.self().id)
    refresh()
    StoryOnboardingDownloadJob.enqueueIfNeeded()
  }

  fun setForceSplitPane(forceSplitPane: Boolean) {
    ZonaRosaStore.internal.forceSplitPane = forceSplitPane
    refresh()
  }

  class Factory(private val repository: InternalSettingsRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return requireNotNull(modelClass.cast(InternalSettingsViewModel(repository)))
    }
  }
}
