package io.zonarosa.messenger.stories.settings.my

import androidx.annotation.WorkerThread
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import io.zonarosa.messenger.database.RecipientTable
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.database.model.DistributionListId
import io.zonarosa.messenger.database.model.DistributionListPrivacyData
import io.zonarosa.messenger.database.model.DistributionListPrivacyMode
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.stories.Stories
import io.zonarosa.messenger.stories.settings.privacy.ChooseInitialMyStoryMembershipState

class MyStorySettingsRepository {

  fun getPrivacyState(): Single<MyStoryPrivacyState> {
    return Single.fromCallable {
      getStoryPrivacyState()
    }.subscribeOn(Schedulers.io())
  }

  fun observeChooseInitialPrivacy(): Observable<ChooseInitialMyStoryMembershipState> {
    return Single
      .fromCallable { ZonaRosaDatabase.distributionLists.getRecipientId(DistributionListId.MY_STORY)!! }
      .subscribeOn(Schedulers.io())
      .flatMapObservable { recipientId ->
        val allZonaRosaConnectionsCount = getAllZonaRosaConnectionsCount().toObservable()
        val stateWithoutCount = Recipient.observable(recipientId)
          .flatMap { Observable.just(ChooseInitialMyStoryMembershipState(recipientId = recipientId, privacyState = getStoryPrivacyState())) }

        Observable.combineLatest(allZonaRosaConnectionsCount, stateWithoutCount) { count, state -> state.copy(allZonaRosaConnectionsCount = count) }
      }
  }

  fun setPrivacyMode(privacyMode: DistributionListPrivacyMode): Completable {
    return Completable.fromAction {
      ZonaRosaDatabase.distributionLists.setPrivacyMode(DistributionListId.MY_STORY, privacyMode)
      Stories.onStorySettingsChanged(DistributionListId.MY_STORY)
    }.subscribeOn(Schedulers.io())
  }

  fun getRepliesAndReactionsEnabled(): Single<Boolean> {
    return Single.fromCallable {
      ZonaRosaDatabase.distributionLists.getStoryType(DistributionListId.MY_STORY).isStoryWithReplies
    }.subscribeOn(Schedulers.io())
  }

  fun setRepliesAndReactionsEnabled(repliesAndReactionsEnabled: Boolean): Completable {
    return Completable.fromAction {
      ZonaRosaDatabase.distributionLists.setAllowsReplies(DistributionListId.MY_STORY, repliesAndReactionsEnabled)
      Stories.onStorySettingsChanged(DistributionListId.MY_STORY)
    }.subscribeOn(Schedulers.io())
  }

  fun getAllZonaRosaConnectionsCount(): Single<Int> {
    return Single.fromCallable {
      ZonaRosaDatabase.recipients.getZonaRosaContactsCount(RecipientTable.IncludeSelfMode.Exclude)
    }.subscribeOn(Schedulers.io())
  }

  @WorkerThread
  private fun getStoryPrivacyState(): MyStoryPrivacyState {
    val privacyData: DistributionListPrivacyData = ZonaRosaDatabase.distributionLists.getPrivacyData(DistributionListId.MY_STORY)

    return MyStoryPrivacyState(
      privacyMode = privacyData.privacyMode,
      connectionCount = privacyData.memberCount
    )
  }
}
