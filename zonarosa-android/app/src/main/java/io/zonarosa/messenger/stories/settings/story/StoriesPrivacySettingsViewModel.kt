package io.zonarosa.messenger.stories.settings.story

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.zonarosa.paging.PagedData
import io.zonarosa.paging.PagingConfig
import io.zonarosa.paging.ProxyPagingController
import io.zonarosa.messenger.contacts.paged.ContactSearchConfiguration
import io.zonarosa.messenger.contacts.paged.ContactSearchKey
import io.zonarosa.messenger.contacts.paged.ContactSearchPagedDataSource
import io.zonarosa.messenger.contacts.paged.ContactSearchPagedDataSourceRepository
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.recipients.RecipientId
import io.zonarosa.messenger.stories.Stories
import io.zonarosa.messenger.util.rx.RxStore

class StoriesPrivacySettingsViewModel(
  contactSearchPagedDataSourceRepository: ContactSearchPagedDataSourceRepository
) : ViewModel() {

  private val repository = StoriesPrivacySettingsRepository()

  private val store = RxStore(
    StoriesPrivacySettingsState(
      areStoriesEnabled = Stories.isFeatureEnabled(),
      areViewReceiptsEnabled = ZonaRosaStore.story.viewedReceiptsEnabled
    )
  )

  private val pagingConfig = PagingConfig.Builder()
    .setBufferPages(1)
    .setPageSize(20)
    .setStartIndex(0)
    .build()

  private val disposables = CompositeDisposable()

  val state: Flowable<StoriesPrivacySettingsState> = store.stateFlowable.observeOn(AndroidSchedulers.mainThread())
  val userHasActiveStories: Boolean get() = store.state.userHasStories
  val pagingController = ProxyPagingController<ContactSearchKey>()

  init {
    val configuration = ContactSearchConfiguration.build {
      addSection(
        ContactSearchConfiguration.Section.Stories(
          includeHeader = false
        )
      )
    }

    val pagedDataSource = ContactSearchPagedDataSource(configuration, contactSearchPagedDataSourceRepository)
    val observablePagedData = PagedData.createForObservable(pagedDataSource, pagingConfig)

    pagingController.set(observablePagedData.controller)

    updateUserHasStories()

    disposables += store.update(observablePagedData.data.toFlowable(BackpressureStrategy.LATEST)) { data, state ->
      state.copy(storyContactItems = data)
    }
  }

  override fun onCleared() {
    disposables.clear()
    store.dispose()
  }

  fun setStoriesEnabled(isEnabled: Boolean) {
    store.update { it.copy(isUpdatingEnabledState = true) }
    disposables += repository.setStoriesEnabled(isEnabled).subscribe {
      store.update {
        it.copy(
          isUpdatingEnabledState = false,
          areStoriesEnabled = Stories.isFeatureEnabled()
        )
      }
      updateUserHasStories()
    }
  }

  fun toggleViewReceipts() {
    ZonaRosaStore.story.viewedReceiptsEnabled = !ZonaRosaStore.story.viewedReceiptsEnabled
    store.update { it.copy(areViewReceiptsEnabled = ZonaRosaStore.story.viewedReceiptsEnabled) }
    repository.onSettingsChanged()
  }

  fun displayGroupsAsStories(recipientIds: List<RecipientId>) {
    disposables += repository.markGroupsAsStories(recipientIds).subscribe {
      pagingController.onDataInvalidated()
    }
  }

  private fun updateUserHasStories() {
    disposables += repository.userHasOutgoingStories().subscribe { userHasActiveStories ->
      store.update { it.copy(userHasStories = userHasActiveStories) }
    }
  }

  class Factory(
    private val contactSearchPagedDataSourceRepository: ContactSearchPagedDataSourceRepository
  ) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return modelClass.cast(StoriesPrivacySettingsViewModel(contactSearchPagedDataSourceRepository)) as T
    }
  }
}
