package io.zonarosa.messenger.stories.settings.group

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import io.zonarosa.messenger.database.GroupTable
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.groups.GroupId
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.storage.StorageSyncHelper

class GroupStorySettingsRepository {
  fun unmarkAsGroupStory(groupId: GroupId): Completable {
    return Completable.fromAction {
      ZonaRosaDatabase.groups.setShowAsStoryState(groupId, GroupTable.ShowAsStoryState.NEVER)
      ZonaRosaDatabase.recipients.markNeedsSync(Recipient.externalGroupExact(groupId).id)
      StorageSyncHelper.scheduleSyncForDataChange()
    }.subscribeOn(Schedulers.io())
  }

  fun getConversationData(groupId: GroupId): Single<GroupConversationData> {
    return Single.fromCallable {
      val recipientId = ZonaRosaDatabase.recipients.getByGroupId(groupId).get()
      val threadId = ZonaRosaDatabase.threads.getThreadIdFor(recipientId) ?: -1L

      GroupConversationData(recipientId, threadId)
    }.subscribeOn(Schedulers.io())
  }
}
