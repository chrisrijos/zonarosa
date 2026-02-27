package io.zonarosa.messenger.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import io.zonarosa.core.util.deleteAll
import io.zonarosa.messenger.database.model.MessageId
import io.zonarosa.messenger.mms.IncomingMessage
import io.zonarosa.messenger.polls.PollOption
import io.zonarosa.messenger.polls.PollRecord
import io.zonarosa.messenger.polls.Voter
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.testing.ZonaRosaActivityRule

@RunWith(AndroidJUnit4::class)
class PollTablesTest {

  @get:Rule
  val harness = ZonaRosaActivityRule()

  private lateinit var poll1: PollRecord

  @Before
  fun setUp() {
    poll1 = PollRecord(
      id = 1,
      question = "how do you feel about unit testing?",
      pollOptions = listOf(
        PollOption(1, "yay", listOf(Voter(1, 1))),
        PollOption(2, "ok", emptyList()),
        PollOption(3, "nay", emptyList())
      ),
      allowMultipleVotes = false,
      hasEnded = false,
      authorId = 1,
      messageId = 1
    )

    ZonaRosaDatabase.polls.writableDatabase.deleteAll(PollTables.PollTable.TABLE_NAME)
    ZonaRosaDatabase.polls.writableDatabase.deleteAll(PollTables.PollOptionTable.TABLE_NAME)
    ZonaRosaDatabase.polls.writableDatabase.deleteAll(PollTables.PollVoteTable.TABLE_NAME)

    val message = IncomingMessage(type = MessageType.NORMAL, from = harness.others[0], sentTimeMillis = 100, serverTimeMillis = 100, receivedTimeMillis = 100)
    ZonaRosaDatabase.messages.insertMessageInbox(message, ZonaRosaDatabase.threads.getOrCreateThreadIdFor(harness.others[0], isGroup = false))
  }

  @Test
  fun givenAPollWithVoting_whenIGetPoll_thenIExpectThatPoll() {
    ZonaRosaDatabase.polls.insertPoll("how do you feel about unit testing?", false, listOf("yay", "ok", "nay"), 1, 1)
    ZonaRosaDatabase.polls.insertVotes(pollId = 1, pollOptionIds = listOf(1), voterId = 1, voteCount = 1, messageId = MessageId(1))

    assertEquals(poll1, ZonaRosaDatabase.polls.getPoll(1))
  }

  @Test
  fun givenAPoll_whenIGetItsOptionIds_thenIExpectAllOptionsIds() {
    ZonaRosaDatabase.polls.insertPoll("how do you feel about unit testing?", false, listOf("yay", "ok", "nay"), 1, 1)
    assertEquals(poll1.pollOptions.map { it.id }, ZonaRosaDatabase.polls.getPollOptionIds(1))
  }

  @Test
  fun givenAPollAndVoter_whenIGetItsVoteCount_thenIExpectTheCorrectVoterCount() {
    ZonaRosaDatabase.polls.insertPoll("how do you feel about unit testing?", false, listOf("yay", "ok", "nay"), 1, 1)
    ZonaRosaDatabase.polls.insertVotes(pollId = 1, pollOptionIds = listOf(1), voterId = 1, voteCount = 1, messageId = MessageId(1))
    ZonaRosaDatabase.polls.insertVotes(pollId = 1, pollOptionIds = listOf(2), voterId = 2, voteCount = 2, messageId = MessageId(1))
    ZonaRosaDatabase.polls.insertVotes(pollId = 1, pollOptionIds = listOf(3), voterId = 3, voteCount = 3, messageId = MessageId(1))

    assertEquals(1, ZonaRosaDatabase.polls.getCurrentPollVoteCount(1, 1))
    assertEquals(2, ZonaRosaDatabase.polls.getCurrentPollVoteCount(1, 2))
    assertEquals(3, ZonaRosaDatabase.polls.getCurrentPollVoteCount(1, 3))
  }

  @Test
  fun givenMultipleRoundsOfVoting_whenIGetItsCount_thenIExpectTheMostRecentResults() {
    ZonaRosaDatabase.polls.insertPoll("how do you feel about unit testing?", false, listOf("yay", "ok", "nay"), 1, 1)
    ZonaRosaDatabase.polls.insertVotes(pollId = 1, pollOptionIds = listOf(2), voterId = 1, voteCount = 1, messageId = MessageId(1))
    ZonaRosaDatabase.polls.insertVotes(pollId = 1, pollOptionIds = listOf(3), voterId = 1, voteCount = 2, messageId = MessageId(1))
    ZonaRosaDatabase.polls.insertVotes(pollId = 1, pollOptionIds = listOf(1), voterId = 1, voteCount = 3, messageId = MessageId(1))

    assertEquals(listOf(Voter(1, 3)), ZonaRosaDatabase.polls.getPoll(1)!!.pollOptions[0].voters)
  }

  @Test
  fun givenAPoll_whenITerminateIt_thenIExpectItToEnd() {
    ZonaRosaDatabase.polls.insertPoll("how do you feel about unit testing?", false, listOf("yay", "ok", "nay"), 1, 1)
    ZonaRosaDatabase.polls.endPoll(1, System.currentTimeMillis())

    assertEquals(true, ZonaRosaDatabase.polls.getPoll(1)!!.hasEnded)
  }

  @Test
  fun givenAPoll_whenIIVote_thenIExpectThatVote() {
    ZonaRosaDatabase.polls.insertPoll("how do you feel about unit testing?", false, listOf("yay", "ok", "nay"), 1, 1)
    val poll = ZonaRosaDatabase.polls.getPoll(1)!!
    val pollOption = poll.pollOptions.first()

    val voteCount = ZonaRosaDatabase.polls.insertVote(poll, pollOption)

    assertEquals(1, voteCount)
    assertEquals(listOf(0), ZonaRosaDatabase.polls.getVotes(poll.id, false, voteCount))
  }

  @Test
  fun givenAPoll_whenIRemoveVote_thenVoteIsCleared() {
    ZonaRosaDatabase.polls.insertPoll("how do you feel about unit testing?", false, listOf("yay", "ok", "nay"), 1, 1)
    val poll = ZonaRosaDatabase.polls.getPoll(1)!!
    val pollOption = poll.pollOptions.first()

    val voteCount = ZonaRosaDatabase.polls.removeVote(poll, pollOption)
    ZonaRosaDatabase.polls.markPendingAsRemoved(poll.id, Recipient.self().id.toLong(), voteCount, 1, pollOption.id)

    assertEquals(1, voteCount)
    val votes = ZonaRosaDatabase.polls.getVotes(poll.id, false, voteCount)
    assertTrue(votes.isEmpty())
  }

  @Test
  fun givenAPendingVote_whenIRevertThatVote_thenItGoesToMostRecentResolvedState() {
    ZonaRosaDatabase.polls.insertPoll("how do you feel about unit testing?", true, listOf("yay", "ok", "nay"), 1, 1)
    val poll = ZonaRosaDatabase.polls.getPoll(1)!!
    val option = poll.pollOptions.first()

    ZonaRosaDatabase.polls.insertVotes(poll.id, listOf(option.id), Recipient.self().id.toLong(), 5, MessageId(1))
    ZonaRosaDatabase.polls.markPendingAsAdded(poll.id, Recipient.self().id.toLong(), 5, 1, option.id)
    ZonaRosaDatabase.polls.removeVote(poll, option)

    ZonaRosaDatabase.polls.removePendingVote(poll.id, option.id, 6, 1)
    val votes = ZonaRosaDatabase.polls.getVotes(1, true, 6)
    assertEquals(listOf(0), votes)
  }
}
