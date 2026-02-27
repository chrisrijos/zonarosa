/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.hasSize
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import io.zonarosa.storageservice.storage.protos.groups.Member
import io.zonarosa.storageservice.storage.protos.groups.local.DecryptedMember
import io.zonarosa.messenger.mms.IncomingMessage
import io.zonarosa.messenger.profiles.ProfileName
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.recipients.RecipientId
import io.zonarosa.messenger.testing.GroupTestingUtils
import io.zonarosa.messenger.testing.ZonaRosaActivityRule

@RunWith(AndroidJUnit4::class)
class NameCollisionTablesTest {

  @get:Rule
  val harness = ZonaRosaActivityRule(createGroup = true)

  private lateinit var alice: RecipientId
  private lateinit var bob: RecipientId
  private lateinit var charlie: RecipientId

  @Before
  fun setUp() {
    alice = setUpRecipient(harness.others[0])
    bob = setUpRecipient(harness.others[1])
    charlie = setUpRecipient(harness.others[2])
  }

  @Test
  fun givenAUserWithAThreadIdButNoConflicts_whenIGetCollisionsForThreadRecipient_thenIExpectNoCollisions() {
    val threadRecipientId = alice
    ZonaRosaDatabase.threads.getOrCreateThreadIdFor(Recipient.resolved(threadRecipientId))
    val actual = ZonaRosaDatabase.nameCollisions.getCollisionsForThreadRecipientId(threadRecipientId)

    assertThat(actual).hasSize(0)
  }

  @Test
  fun givenTwoUsers_whenOneChangesTheirProfileNameToMatchTheOther_thenIExpectANameCollision() {
    setProfileName(alice, ProfileName.fromParts("Alice", "Android"))
    setProfileName(bob, ProfileName.fromParts("Bob", "Android"))
    setProfileName(alice, ProfileName.fromParts("Bob", "Android"))

    val actualAlice = ZonaRosaDatabase.nameCollisions.getCollisionsForThreadRecipientId(alice)
    val actualBob = ZonaRosaDatabase.nameCollisions.getCollisionsForThreadRecipientId(bob)

    assertThat(actualAlice).hasSize(2)
    assertThat(actualBob).hasSize(2)
  }

  @Test
  fun givenTwoUsersWithANameCollisions_whenOneChangesToADifferentName_thenIExpectNoNameCollisions() {
    setProfileName(alice, ProfileName.fromParts("Bob", "Android"))
    setProfileName(bob, ProfileName.fromParts("Bob", "Android"))
    setProfileName(alice, ProfileName.fromParts("Alice", "Android"))

    val actualAlice = ZonaRosaDatabase.nameCollisions.getCollisionsForThreadRecipientId(alice)
    val actualBob = ZonaRosaDatabase.nameCollisions.getCollisionsForThreadRecipientId(bob)

    assertThat(actualAlice).hasSize(0)
    assertThat(actualBob).hasSize(0)
  }

  @Test
  fun givenThreeUsersWithANameCollisions_whenOneChangesToADifferentName_thenIExpectTwoNameCollisions() {
    setProfileName(alice, ProfileName.fromParts("Bob", "Android"))
    setProfileName(bob, ProfileName.fromParts("Bob", "Android"))
    setProfileName(charlie, ProfileName.fromParts("Bob", "Android"))
    setProfileName(alice, ProfileName.fromParts("Alice", "Android"))

    val actualAlice = ZonaRosaDatabase.nameCollisions.getCollisionsForThreadRecipientId(alice)
    val actualBob = ZonaRosaDatabase.nameCollisions.getCollisionsForThreadRecipientId(bob)
    val actualCharlie = ZonaRosaDatabase.nameCollisions.getCollisionsForThreadRecipientId(charlie)

    assertThat(actualAlice).hasSize(0)
    assertThat(actualBob).hasSize(2)
    assertThat(actualCharlie).hasSize(2)
  }

  @Test
  fun givenTwoUsersWithADismissedNameCollision_whenOneChangesToADifferentNameAndBack_thenIExpectANameCollision() {
    setProfileName(alice, ProfileName.fromParts("Bob", "Android"))
    setProfileName(bob, ProfileName.fromParts("Bob", "Android"))
    ZonaRosaDatabase.nameCollisions.markCollisionsForThreadRecipientDismissed(alice)

    setProfileName(alice, ProfileName.fromParts("Alice", "Android"))
    setProfileName(alice, ProfileName.fromParts("Bob", "Android"))

    val actualAlice = ZonaRosaDatabase.nameCollisions.getCollisionsForThreadRecipientId(alice)

    assertThat(actualAlice).hasSize(2)
  }

  @Test
  fun givenADismissedNameCollisionForAlice_whenIGetNameCollisionsForAlice_thenIExpectNoNameCollisions() {
    setProfileName(alice, ProfileName.fromParts("Bob", "Android"))
    setProfileName(bob, ProfileName.fromParts("Bob", "Android"))
    ZonaRosaDatabase.nameCollisions.markCollisionsForThreadRecipientDismissed(alice)

    val actualCollisions = ZonaRosaDatabase.nameCollisions.getCollisionsForThreadRecipientId(alice)

    assertThat(actualCollisions).hasSize(0)
  }

  @Test
  fun givenADismissedNameCollisionForAliceThatIUpdate_whenIGetNameCollisionsForAlice_thenIExpectNoNameCollisions() {
    ZonaRosaDatabase.threads.getOrCreateThreadIdFor(Recipient.resolved(alice))

    setProfileName(alice, ProfileName.fromParts("Bob", "Android"))
    setProfileName(bob, ProfileName.fromParts("Bob", "Android"))
    ZonaRosaDatabase.nameCollisions.markCollisionsForThreadRecipientDismissed(alice)
    setProfileName(bob, ProfileName.fromParts("Bob", "Android"))

    val actualCollisions = ZonaRosaDatabase.nameCollisions.getCollisionsForThreadRecipientId(alice)

    assertThat(actualCollisions).hasSize(0)
  }

  @Test
  fun givenADismissedNameCollisionForAlice_whenIGetNameCollisionsForBob_thenIExpectANameCollisionWithTwoEntries() {
    ZonaRosaDatabase.threads.getOrCreateThreadIdFor(Recipient.resolved(alice))

    setProfileName(alice, ProfileName.fromParts("Bob", "Android"))
    setProfileName(bob, ProfileName.fromParts("Bob", "Android"))
    ZonaRosaDatabase.nameCollisions.markCollisionsForThreadRecipientDismissed(alice)

    val actualCollisions = ZonaRosaDatabase.nameCollisions.getCollisionsForThreadRecipientId(bob)

    assertThat(actualCollisions).hasSize(2)
  }

  @Test
  fun givenAGroupWithAliceAndBob_whenIInsertNameChangeMessageForAlice_thenIExpectAGroupNameCollision() {
    val alice = Recipient.resolved(alice)
    val bob = Recipient.resolved(bob)
    val info = createGroup()

    setProfileName(alice.id, ProfileName.fromParts("Bob", "Android"))
    setProfileName(bob.id, ProfileName.fromParts("Bob", "Android"))

    ZonaRosaDatabase.threads.getOrCreateThreadIdFor(Recipient.resolved(info.recipientId))
    ZonaRosaDatabase.messages.insertProfileNameChangeMessages(alice, "Bob Android", "Alice Android")

    val collisions = ZonaRosaDatabase.nameCollisions.getCollisionsForThreadRecipientId(info.recipientId)

    assertThat(collisions).hasSize(2)
  }

  @Test
  fun givenAGroupWithAliceAndBobWithDismissedCollision_whenIInsertNameChangeMessageForAlice_thenIExpectAGroupNameCollision() {
    val alice = Recipient.resolved(alice)
    val bob = Recipient.resolved(bob)
    val info = createGroup()

    setProfileName(alice.id, ProfileName.fromParts("Bob", "Android"))
    setProfileName(bob.id, ProfileName.fromParts("Bob", "Android"))

    ZonaRosaDatabase.threads.getOrCreateThreadIdFor(Recipient.resolved(info.recipientId))
    ZonaRosaDatabase.messages.insertProfileNameChangeMessages(alice, "Bob Android", "Alice Android")
    ZonaRosaDatabase.nameCollisions.markCollisionsForThreadRecipientDismissed(info.recipientId)
    ZonaRosaDatabase.messages.insertProfileNameChangeMessages(alice, "Bob Android", "Alice Android")

    val collisions = ZonaRosaDatabase.nameCollisions.getCollisionsForThreadRecipientId(info.recipientId)

    assertThat(collisions).hasSize(0)
  }

  @Test
  fun givenAGroupWithAliceAndBob_whenIInsertNameChangeMessageForAliceWithMismatch_thenIExpectNoGroupNameCollision() {
    val alice = Recipient.resolved(alice)
    val bob = Recipient.resolved(bob)
    val info = createGroup()

    setProfileName(alice.id, ProfileName.fromParts("Alice", "Android"))
    setProfileName(bob.id, ProfileName.fromParts("Bob", "Android"))

    ZonaRosaDatabase.threads.getOrCreateThreadIdFor(Recipient.resolved(info.recipientId))
    ZonaRosaDatabase.messages.insertProfileNameChangeMessages(alice, "Alice Android", "Bob Android")

    val collisions = ZonaRosaDatabase.nameCollisions.getCollisionsForThreadRecipientId(info.recipientId)

    assertThat(collisions).hasSize(0)
  }

  private fun setUpRecipient(recipientId: RecipientId): RecipientId {
    ZonaRosaDatabase.recipients.setProfileSharing(recipientId, false)
    val threadId = ZonaRosaDatabase.threads.getOrCreateThreadIdFor(recipientId, false)

    MmsHelper.insert(
      threadId = threadId,
      message = IncomingMessage(
        type = MessageType.NORMAL,
        from = recipientId,
        groupId = null,
        body = "hi",
        sentTimeMillis = 100L,
        receivedTimeMillis = 200L,
        serverTimeMillis = 100L,
        isUnidentified = true
      )
    )

    return recipientId
  }

  private fun setProfileName(recipientId: RecipientId, name: ProfileName) {
    ZonaRosaDatabase.recipients.setProfileName(recipientId, name)
    Recipient.live(recipientId).refresh()
    ZonaRosaDatabase.nameCollisions.handleIndividualNameCollision(recipientId)
  }

  private fun createGroup(): GroupTestingUtils.TestGroupInfo {
    return GroupTestingUtils.insertGroup(
      revision = 0,
      DecryptedMember(
        aciBytes = harness.self.requireAci().toByteString(),
        role = Member.Role.ADMINISTRATOR
      ),
      DecryptedMember(
        aciBytes = Recipient.resolved(alice).requireAci().toByteString(),
        role = Member.Role.ADMINISTRATOR
      ),
      DecryptedMember(
        aciBytes = Recipient.resolved(bob).requireAci().toByteString(),
        role = Member.Role.ADMINISTRATOR
      )
    )
  }
}
