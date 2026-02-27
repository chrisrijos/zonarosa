package io.zonarosa.messenger.contacts.paged

import android.content.Context
import android.database.Cursor
import androidx.annotation.WorkerThread
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import io.zonarosa.core.util.CursorUtil
import io.zonarosa.messenger.R
import io.zonarosa.messenger.contacts.ContactRepository
import io.zonarosa.messenger.contacts.paged.collections.ContactSearchIterator
import io.zonarosa.messenger.database.DistributionListTables
import io.zonarosa.messenger.database.GroupTable
import io.zonarosa.messenger.database.RecipientTable
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.database.ThreadTable
import io.zonarosa.messenger.database.model.DistributionListPrivacyMode
import io.zonarosa.messenger.database.model.GroupRecord
import io.zonarosa.messenger.groups.GroupsInCommonRepository
import io.zonarosa.messenger.groups.GroupsInCommonSummary
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.keyvalue.StorySend
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.recipients.RecipientId

/**
 * Database boundary interface which allows us to safely unit test the data source without
 * having to deal with database access.
 */
open class ContactSearchPagedDataSourceRepository(
  context: Context
) {

  private val contactRepository = ContactRepository(context.getString(R.string.note_to_self))
  private val context = context.applicationContext

  open fun getLatestStorySends(activeStoryCutoffDuration: Long): List<StorySend> {
    return ZonaRosaStore.story
      .getLatestActiveStorySendTimestamps(System.currentTimeMillis() - activeStoryCutoffDuration)
  }

  open fun queryZonaRosaContacts(contactsSearchQuery: RecipientTable.ContactSearchQuery): Cursor? {
    return contactRepository.queryZonaRosaContacts(contactsSearchQuery)
  }

  open fun queryZonaRosaContactLetterHeaders(query: String?, includeSelfMode: RecipientTable.IncludeSelfMode, includePush: Boolean, includeSms: Boolean): Map<RecipientId, String> {
    return ZonaRosaDatabase.recipients.queryZonaRosaContactLetterHeaders(query ?: "", includeSelfMode, includePush, includeSms)
  }

  open fun queryGroupMemberContacts(query: String?): Cursor? {
    return contactRepository.queryGroupMemberContacts(query ?: "")
  }

  open fun getGroupSearchIterator(
    section: ContactSearchConfiguration.Section.Groups,
    query: String?
  ): ContactSearchIterator<GroupRecord> {
    return ZonaRosaDatabase.groups.queryGroups(
      GroupTable.GroupQuery.Builder()
        .withSearchQuery(query)
        .withInactiveGroups(section.includeInactive)
        .withMmsGroups(section.includeMms)
        .withV1Groups(section.includeV1)
        .withSortOrder(section.sortOrder)
        .build()
    )
  }

  open fun getRecents(section: ContactSearchConfiguration.Section.Recents): Cursor? {
    return ZonaRosaDatabase.threads.getRecentConversationList(
      section.limit,
      section.includeInactiveGroups,
      section.mode == ContactSearchConfiguration.Section.Recents.Mode.INDIVIDUALS,
      section.mode == ContactSearchConfiguration.Section.Recents.Mode.GROUPS,
      !section.includeGroupsV1,
      !section.includeSms,
      !section.includeSelf
    )
  }

  open fun getStories(query: String?): Cursor? {
    return ZonaRosaDatabase.distributionLists.getAllListsForContactSelectionUiCursor(query, myStoryContainsQuery(query ?: ""))
  }

  open fun getGroupsWithMembers(query: String): Cursor {
    return ZonaRosaDatabase.groups.queryGroupsByMemberName(query)
  }

  open fun getContactsWithoutThreads(query: String): Cursor {
    return ZonaRosaDatabase.recipients.getAllContactsWithoutThreads(query)
  }

  open fun getRecipientFromDistributionListCursor(cursor: Cursor): Recipient {
    return Recipient.resolved(RecipientId.from(CursorUtil.requireLong(cursor, DistributionListTables.RECIPIENT_ID)))
  }

  open fun getPrivacyModeFromDistributionListCursor(cursor: Cursor): DistributionListPrivacyMode {
    return DistributionListPrivacyMode.deserialize(CursorUtil.requireLong(cursor, DistributionListTables.PRIVACY_MODE))
  }

  open fun getRecipientFromThreadCursor(cursor: Cursor): Recipient {
    return Recipient.resolved(RecipientId.from(CursorUtil.requireLong(cursor, ThreadTable.RECIPIENT_ID)))
  }

  open fun getRecipientFromSearchCursor(cursor: Cursor): Recipient {
    return Recipient.resolved(RecipientId.from(CursorUtil.requireLong(cursor, ContactRepository.ID_COLUMN)))
  }

  open fun getRecipientFromRecipientCursor(cursor: Cursor): Recipient {
    return Recipient.resolved(RecipientId.from(CursorUtil.requireLong(cursor, RecipientTable.ID)))
  }

  @WorkerThread
  open fun getGroupsInCommon(recipient: Recipient): GroupsInCommonSummary {
    return runBlocking {
      GroupsInCommonRepository
        .getGroupsInCommonSummary(context, recipient.id)
        .first()
    }
  }

  open fun getRecipientFromGroupRecord(groupRecord: GroupRecord): Recipient {
    return Recipient.resolved(groupRecord.recipientId)
  }

  open fun getDistributionListMembershipCount(recipient: Recipient): Int {
    return ZonaRosaDatabase.distributionLists.getMemberCount(recipient.requireDistributionListId())
  }

  open fun getGroupStories(): Set<ContactSearchData.Story> {
    return ZonaRosaDatabase.groups.getGroupsToDisplayAsStories().map {
      val recipient = Recipient.resolved(ZonaRosaDatabase.recipients.getOrInsertFromGroupId(it))
      ContactSearchData.Story(recipient, recipient.participantIds.size, DistributionListPrivacyMode.ALL)
    }.toSet()
  }

  open fun recipientNameContainsQuery(recipient: Recipient, query: String?): Boolean {
    return query.isNullOrBlank() || recipient.getDisplayName(context).contains(query, ignoreCase = true)
  }

  open fun myStoryContainsQuery(query: String): Boolean {
    if (query.isEmpty()) {
      return true
    }

    val myStory = context.getString(R.string.Recipient_my_story)
    return myStory.contains(query, ignoreCase = true)
  }
}
