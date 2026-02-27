package io.zonarosa.messenger.components.settings.app.usernamelinks.colorpicker

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.collections.immutable.toImmutableList
import io.zonarosa.core.util.concurrent.ZonaRosaExecutors
import io.zonarosa.messenger.components.settings.app.usernamelinks.QrCodeData
import io.zonarosa.messenger.components.settings.app.usernamelinks.QrCodeState
import io.zonarosa.messenger.components.settings.app.usernamelinks.UsernameQrCodeColorScheme
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.profiles.manage.UsernameRepository.toLink
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.storage.StorageSyncHelper

class UsernameLinkQrColorPickerViewModel : ViewModel() {

  private val _state = mutableStateOf(
    UsernameLinkQrColorPickerState(
      username = ZonaRosaStore.account.username!!,
      qrCodeData = QrCodeState.Loading,
      colorSchemes = UsernameQrCodeColorScheme.entries.toImmutableList(),
      selectedColorScheme = ZonaRosaStore.misc.usernameQrCodeColorScheme
    )
  )

  val state: State<UsernameLinkQrColorPickerState> = _state

  private val disposable: CompositeDisposable = CompositeDisposable()

  init {
    val usernameLink = ZonaRosaStore.account.usernameLink

    if (usernameLink != null) {
      disposable += Single
        .fromCallable { QrCodeData.forData(usernameLink.toLink()) }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { qrData ->
          _state.value = _state.value.copy(
            qrCodeData = QrCodeState.Present(qrData)
          )
        }
    } else {
      _state.value = _state.value.copy(
        qrCodeData = QrCodeState.NotSet
      )
    }
  }

  override fun onCleared() {
    disposable.clear()
  }

  fun onColorSelected(color: UsernameQrCodeColorScheme) {
    ZonaRosaStore.misc.usernameQrCodeColorScheme = color
    ZonaRosaExecutors.BOUNDED.run {
      ZonaRosaDatabase.recipients.markNeedsSync(Recipient.self().id)
      StorageSyncHelper.scheduleSyncForDataChange()
    }

    _state.value = _state.value.copy(
      selectedColorScheme = color
    )
  }
}
