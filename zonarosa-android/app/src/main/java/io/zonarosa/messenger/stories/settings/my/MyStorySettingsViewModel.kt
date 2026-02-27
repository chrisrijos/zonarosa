package io.zonarosa.messenger.stories.settings.my

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.schedulers.Schedulers
import io.zonarosa.messenger.database.model.DistributionListPrivacyMode
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.stories.Stories
import io.zonarosa.messenger.util.livedata.Store

class MyStorySettingsViewModel @JvmOverloads constructor(private val repository: MyStorySettingsRepository = MyStorySettingsRepository()) : ViewModel() {
  private val store = Store(MyStorySettingsState(hasUserPerformedManualSelection = ZonaRosaStore.story.userHasBeenNotifiedAboutStories))
  private val disposables = CompositeDisposable()

  val state: LiveData<MyStorySettingsState> = store.stateLiveData

  override fun onCleared() {
    disposables.clear()
  }

  fun refresh() {
    disposables.clear()
    disposables += repository.getPrivacyState()
      .subscribe { myStoryPrivacyState -> store.update { it.copy(myStoryPrivacyState = myStoryPrivacyState) } }
    disposables += repository.getRepliesAndReactionsEnabled()
      .subscribe { repliesAndReactionsEnabled -> store.update { it.copy(areRepliesAndReactionsEnabled = repliesAndReactionsEnabled) } }
    disposables += repository.getAllZonaRosaConnectionsCount()
      .subscribe { allZonaRosaConnectionsCount -> store.update { it.copy(allZonaRosaConnectionsCount = allZonaRosaConnectionsCount) } }
  }

  fun setRepliesAndReactionsEnabled(repliesAndReactionsEnabled: Boolean) {
    disposables += repository.setRepliesAndReactionsEnabled(repliesAndReactionsEnabled)
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe { refresh() }
  }

  fun setMyStoryPrivacyMode(privacyMode: DistributionListPrivacyMode): Completable {
    store.update { state ->
      state.copy(hasUserPerformedManualSelection = true)
    }

    ZonaRosaStore.story.userHasBeenNotifiedAboutStories = true

    return if (privacyMode == state.value!!.myStoryPrivacyState.privacyMode) {
      Completable.fromAction {
        Stories.onStorySettingsChanged(Recipient.self().id)
      }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    } else {
      repository.setPrivacyMode(privacyMode)
        .observeOn(AndroidSchedulers.mainThread())
        .doOnComplete { refresh() }
    }
  }
}
