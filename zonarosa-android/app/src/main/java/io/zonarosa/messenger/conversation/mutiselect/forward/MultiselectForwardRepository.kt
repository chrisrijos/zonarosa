package io.zonarosa.messenger.conversation.mutiselect.forward

import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import io.zonarosa.core.util.concurrent.ZonaRosaExecutors
import io.zonarosa.messenger.contacts.paged.ContactSearchKey
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.recipients.RecipientId
import io.zonarosa.messenger.sharing.MultiShareArgs
import io.zonarosa.messenger.sharing.MultiShareSender
import io.zonarosa.messenger.stories.Stories
import io.zonarosa.service.api.util.Preconditions
import java.util.Optional

class MultiselectForwardRepository {

  class MultiselectForwardResultHandlers(
    val onAllMessageSentSuccessfully: () -> Unit,
    val onSomeMessagesFailed: () -> Unit,
    val onAllMessagesFailed: () -> Unit
  )

  fun checkAllSelectedMediaCanBeSentToStories(records: List<MultiShareArgs>): Single<Stories.MediaTransform.SendRequirements> {
    Preconditions.checkArgument(records.isNotEmpty())

    if (!Stories.isFeatureEnabled()) {
      return Single.just(Stories.MediaTransform.SendRequirements.CAN_NOT_SEND)
    }

    return Single.fromCallable {
      if (records.any { !it.isValidForStories }) {
        Stories.MediaTransform.SendRequirements.CAN_NOT_SEND
      } else {
        Stories.MediaTransform.getSendRequirements(records.map { it.media }.flatten())
      }
    }.subscribeOn(Schedulers.io())
  }

  fun canSelectRecipient(recipientId: Optional<RecipientId>): Single<Boolean> {
    if (!recipientId.isPresent) {
      return Single.just(true)
    }

    return Single.fromCallable {
      val recipient = Recipient.resolved(recipientId.get())
      if (recipient.isPushV2Group) {
        val record = ZonaRosaDatabase.groups.getGroup(recipient.requireGroupId())
        !(record.isPresent && record.get().isAnnouncementGroup && !record.get().isAdmin(Recipient.self()))
      } else {
        true
      }
    }
  }

  fun send(
    additionalMessage: String,
    multiShareArgs: List<MultiShareArgs>,
    shareContacts: Set<ContactSearchKey>,
    resultHandlers: MultiselectForwardResultHandlers
  ) {
    ZonaRosaExecutors.BOUNDED.execute {
      val filteredContacts: Set<ContactSearchKey> = shareContacts
        .asSequence()
        .filter { it is ContactSearchKey.RecipientSearchKey }
        .toSet()

      val mappedArgs: List<MultiShareArgs> = multiShareArgs.map { it.buildUpon(filteredContacts).build() }
      val results = mappedArgs.sortedBy { it.timestamp }.map { MultiShareSender.sendSync(it) }

      if (additionalMessage.isNotEmpty()) {
        val additional = MultiShareArgs.Builder(filteredContacts.filterNot { it is ContactSearchKey.RecipientSearchKey && it.isStory }.toSet())
          .withDraftText(additionalMessage)
          .build()

        if (additional.contactSearchKeys.isNotEmpty()) {
          val additionalResult: MultiShareSender.MultiShareSendResultCollection = MultiShareSender.sendSync(additional)

          handleResults(results + additionalResult, resultHandlers)
        } else {
          handleResults(results, resultHandlers)
        }
      } else {
        handleResults(results, resultHandlers)
      }
    }
  }

  private fun handleResults(
    results: List<MultiShareSender.MultiShareSendResultCollection>,
    resultHandlers: MultiselectForwardResultHandlers
  ) {
    if (results.any { it.containsFailures() }) {
      if (results.all { it.containsOnlyFailures() }) {
        resultHandlers.onAllMessagesFailed()
      } else {
        resultHandlers.onSomeMessagesFailed()
      }
    } else {
      resultHandlers.onAllMessageSentSuccessfully()
    }
  }
}
