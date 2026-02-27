package io.zonarosa.messenger.components.settings.conversation

import android.content.Context
import android.database.Cursor
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.rx3.asObservable
import io.zonarosa.core.util.concurrent.ZonaRosaExecutors
import io.zonarosa.core.util.logging.Log
import io.zonarosa.storageservice.storage.protos.groups.local.DecryptedGroup
import io.zonarosa.messenger.contacts.sync.ContactDiscovery
import io.zonarosa.messenger.database.CallTable
import io.zonarosa.messenger.database.MediaTable
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.database.model.GroupRecord
import io.zonarosa.messenger.database.model.IdentityRecord
import io.zonarosa.messenger.database.model.MessageRecord
import io.zonarosa.messenger.database.model.StoryViewState
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.groups.GroupId
import io.zonarosa.messenger.groups.GroupProtoUtil
import io.zonarosa.messenger.groups.GroupsInCommonRepository
import io.zonarosa.messenger.groups.LiveGroup
import io.zonarosa.messenger.groups.ui.GroupChangeFailureReason
import io.zonarosa.messenger.groups.ui.GroupChangeResult
import io.zonarosa.messenger.groups.v2.GroupAddMembersResult
import io.zonarosa.messenger.groups.v2.GroupManagementRepository
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.recipients.RecipientId
import io.zonarosa.messenger.recipients.RecipientUtil
import io.zonarosa.messenger.util.RemoteConfig
import java.io.IOException

private val TAG = Log.tag(ConversationSettingsRepository::class.java)

class ConversationSettingsRepository(
  private val context: Context,
  private val groupManagementRepository: GroupManagementRepository = GroupManagementRepository(context)
) {

  fun getCallEvents(callRowIds: LongArray): Single<List<Pair<CallTable.Call, MessageRecord>>> {
    return if (callRowIds.isEmpty()) {
      Single.just(emptyList())
    } else {
      Single.fromCallable {
        val callMap = ZonaRosaDatabase.calls.getCallsByRowIds(callRowIds.toList())
        val messageIds = callMap.values.mapNotNull { it.messageId }
        ZonaRosaDatabase.messages.getMessages(messageIds).iterator().asSequence()
          .filter { callMap.containsKey(it.id) }
          .map { callMap[it.id]!! to it }
          .sortedByDescending { it.first.timestamp }
          .toList()
      }
    }
  }

  @WorkerThread
  fun getThreadMedia(threadId: Long, limit: Int): Cursor? {
    return if (threadId > 0) {
      ZonaRosaDatabase.media.getGalleryMediaForThread(threadId, MediaTable.Sorting.Newest, limit)
    } else {
      null
    }
  }

  fun getStoryViewState(groupId: GroupId): Observable<StoryViewState> {
    return Observable.fromCallable {
      ZonaRosaDatabase.recipients.getByGroupId(groupId)
    }.flatMap {
      StoryViewState.getForRecipientId(it.get())
    }.observeOn(Schedulers.io())
  }

  fun getThreadId(recipientId: RecipientId, consumer: (Long) -> Unit) {
    ZonaRosaExecutors.BOUNDED.execute {
      consumer(ZonaRosaDatabase.threads.getThreadIdIfExistsFor(recipientId))
    }
  }

  fun getThreadId(groupId: GroupId, consumer: (Long) -> Unit) {
    ZonaRosaExecutors.BOUNDED.execute {
      val recipientId = Recipient.externalGroupExact(groupId).id
      consumer(ZonaRosaDatabase.threads.getThreadIdIfExistsFor(recipientId))
    }
  }

  fun isInternalRecipientDetailsEnabled(): Boolean = ZonaRosaStore.internal.recipientDetails

  fun hasGroups(consumer: (Boolean) -> Unit) {
    ZonaRosaExecutors.BOUNDED.execute { consumer(ZonaRosaDatabase.groups.getActiveGroupCount() > 0) }
  }

  fun getIdentity(recipientId: RecipientId, consumer: (IdentityRecord?) -> Unit) {
    ZonaRosaExecutors.BOUNDED.execute {
      if (ZonaRosaStore.account.aci != null && ZonaRosaStore.account.pni != null) {
        consumer(AppDependencies.protocolStore.aci().identities().getIdentityRecord(recipientId).orElse(null))
      } else {
        consumer(null)
      }
    }
  }

  fun getGroupsInCommon(recipientId: RecipientId): Observable<List<Recipient>> {
    return GroupsInCommonRepository.getGroupsInCommon(context, recipientId)
      .asObservable()
  }

  fun getGroupMembership(recipientId: RecipientId, consumer: (List<RecipientId>) -> Unit) {
    ZonaRosaExecutors.BOUNDED.execute {
      val groupDatabase = ZonaRosaDatabase.groups
      val groupRecords = groupDatabase.getPushGroupsContainingMember(recipientId)
      val groupRecipients = ArrayList<RecipientId>(groupRecords.size)
      for (groupRecord in groupRecords) {
        groupRecipients.add(groupRecord.recipientId)
      }
      consumer(groupRecipients)
    }
  }

  fun refreshRecipient(recipientId: RecipientId) {
    ZonaRosaExecutors.UNBOUNDED.execute {
      try {
        ContactDiscovery.refresh(context, Recipient.resolved(recipientId), false)
      } catch (e: IOException) {
        Log.w(TAG, "Failed to refresh user after adding to contacts.")
      }
    }
  }

  fun setMuteUntil(recipientId: RecipientId, until: Long) {
    ZonaRosaExecutors.BOUNDED.execute {
      ZonaRosaDatabase.recipients.setMuted(recipientId, until)
    }
  }

  fun getGroupCapacity(groupId: GroupId, consumer: (GroupCapacityResult) -> Unit) {
    ZonaRosaExecutors.BOUNDED.execute {
      val groupRecord: GroupRecord = ZonaRosaDatabase.groups.getGroup(groupId).get()
      consumer(
        if (groupRecord.isV2Group) {
          val decryptedGroup: DecryptedGroup = groupRecord.requireV2GroupProperties().decryptedGroup
          val pendingMembers: List<RecipientId> = decryptedGroup.pendingMembers
            .map { m -> m.serviceIdBytes }
            .map { s -> GroupProtoUtil.serviceIdBinaryToRecipientId(s) }

          val members = mutableListOf<RecipientId>()

          members.addAll(groupRecord.members)
          members.addAll(pendingMembers)

          GroupCapacityResult(Recipient.self().id, members, RemoteConfig.groupLimits, groupRecord.isAnnouncementGroup)
        } else {
          GroupCapacityResult(Recipient.self().id, groupRecord.members, RemoteConfig.groupLimits, false)
        }
      )
    }
  }

  fun addMembers(groupId: GroupId, selected: List<RecipientId>, consumer: (GroupAddMembersResult) -> Unit) {
    groupManagementRepository.addMembers(groupId, selected, consumer)
  }

  fun setMuteUntil(groupId: GroupId, until: Long) {
    ZonaRosaExecutors.BOUNDED.execute {
      val recipientId = Recipient.externalGroupExact(groupId).id
      ZonaRosaDatabase.recipients.setMuted(recipientId, until)
    }
  }

  @WorkerThread
  fun block(recipientId: RecipientId): GroupChangeResult {
    return try {
      val recipient = Recipient.resolved(recipientId)
      if (recipient.isGroup) {
        RecipientUtil.block(context, recipient)
      } else {
        RecipientUtil.blockNonGroup(context, recipient)
      }
      GroupChangeResult.SUCCESS
    } catch (e: Exception) {
      Log.w(TAG, "Failed to block recipient.", e)
      GroupChangeResult.failure(GroupChangeFailureReason.fromException(e))
    }
  }

  fun unblock(recipientId: RecipientId) {
    ZonaRosaExecutors.BOUNDED.execute {
      val recipient = Recipient.resolved(recipientId)
      RecipientUtil.unblock(recipient)
    }
  }

  @WorkerThread
  fun block(groupId: GroupId): GroupChangeResult {
    return try {
      val recipient = Recipient.externalGroupExact(groupId)
      RecipientUtil.block(context, recipient)
      GroupChangeResult.SUCCESS
    } catch (e: Exception) {
      Log.w(TAG, "Failed to block group.", e)
      GroupChangeResult.failure(GroupChangeFailureReason.fromException(e))
    }
  }

  fun unblock(groupId: GroupId) {
    ZonaRosaExecutors.BOUNDED.execute {
      val recipient = Recipient.externalGroupExact(groupId)
      RecipientUtil.unblock(recipient)
    }
  }

  @WorkerThread
  fun isMessageRequestAccepted(recipient: Recipient): Boolean {
    return RecipientUtil.isMessageRequestAccepted(context, recipient)
  }

  fun getMembershipCountDescription(liveGroup: LiveGroup): LiveData<String> {
    return liveGroup.getMembershipCountDescription(context.resources)
  }

  fun getExternalPossiblyMigratedGroupRecipientId(groupId: GroupId, consumer: (RecipientId) -> Unit) {
    ZonaRosaExecutors.BOUNDED.execute {
      consumer(Recipient.externalPossiblyMigratedGroup(groupId).id)
    }
  }
}
