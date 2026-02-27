package io.zonarosa.messenger.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import io.zonarosa.core.models.ServiceId.ACI
import io.zonarosa.core.models.ServiceId.PNI
import io.zonarosa.core.util.CursorUtil
import io.zonarosa.messenger.profiles.ProfileName
import io.zonarosa.messenger.recipients.RecipientId
import io.zonarosa.messenger.testing.ZonaRosaActivityRule
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class RecipientTableTest {

  @get:Rule
  val harness = ZonaRosaActivityRule()

  @Test
  fun givenAHiddenRecipient_whenIQueryAllContacts_thenIExpectHiddenToBeReturned() {
    val hiddenRecipient = harness.others[0]
    ZonaRosaDatabase.recipients.setProfileName(hiddenRecipient, ProfileName.fromParts("Hidden", "Person"))
    ZonaRosaDatabase.recipients.markHidden(hiddenRecipient)

    val results = ZonaRosaDatabase.recipients.queryAllContacts("Hidden", RecipientTable.IncludeSelfMode.Exclude)!!

    assertEquals(1, results.count)
  }

  @Test
  fun givenAHiddenRecipient_whenIGetZonaRosaContacts_thenIDoNotExpectHiddenToBeReturned() {
    val hiddenRecipient = harness.others[0]
    ZonaRosaDatabase.recipients.setProfileName(hiddenRecipient, ProfileName.fromParts("Hidden", "Person"))
    ZonaRosaDatabase.recipients.markHidden(hiddenRecipient)

    val results: MutableList<RecipientId> = ZonaRosaDatabase.recipients.getZonaRosaContacts(RecipientTable.IncludeSelfMode.Exclude).use {
      val ids = mutableListOf<RecipientId>()
      while (it.moveToNext()) {
        ids.add(RecipientId.from(CursorUtil.requireLong(it, RecipientTable.ID)))
      }

      ids
    }!!

    assertNotEquals(0, results.size)
    assertFalse(hiddenRecipient in results)
  }

  @Test
  fun givenAHiddenRecipient_whenIQueryZonaRosaContacts_thenIDoNotExpectHiddenToBeReturned() {
    val hiddenRecipient = harness.others[0]
    ZonaRosaDatabase.recipients.setProfileName(hiddenRecipient, ProfileName.fromParts("Hidden", "Person"))
    ZonaRosaDatabase.recipients.markHidden(hiddenRecipient)

    val results = ZonaRosaDatabase.recipients.queryZonaRosaContacts(RecipientTable.ContactSearchQuery("Hidden", RecipientTable.IncludeSelfMode.Exclude))!!

    assertEquals(0, results.count)
  }

  @Test
  fun givenAHiddenRecipient_whenIGetNonGroupContacts_thenIDoNotExpectHiddenToBeReturned() {
    val hiddenRecipient = harness.others[0]
    ZonaRosaDatabase.recipients.setProfileName(hiddenRecipient, ProfileName.fromParts("Hidden", "Person"))
    ZonaRosaDatabase.recipients.markHidden(hiddenRecipient)

    val results: MutableList<RecipientId> = ZonaRosaDatabase.recipients.getNonGroupContacts(RecipientTable.IncludeSelfMode.Exclude)?.use {
      val ids = mutableListOf<RecipientId>()
      while (it.moveToNext()) {
        ids.add(RecipientId.from(CursorUtil.requireLong(it, RecipientTable.ID)))
      }

      ids
    }!!

    assertNotEquals(0, results.size)
    assertFalse(hiddenRecipient in results)
  }

  @Test
  fun givenABlockedRecipient_whenIQueryAllContacts_thenIDoNotExpectBlockedToBeReturned() {
    val blockedRecipient = harness.others[0]
    ZonaRosaDatabase.recipients.setProfileName(blockedRecipient, ProfileName.fromParts("Blocked", "Person"))
    ZonaRosaDatabase.recipients.setBlocked(blockedRecipient, true)

    val results = ZonaRosaDatabase.recipients.queryAllContacts("Blocked", RecipientTable.IncludeSelfMode.Exclude)!!

    assertEquals(0, results.count)
  }

  @Test
  fun givenABlockedRecipient_whenIGetZonaRosaContacts_thenIDoNotExpectBlockedToBeReturned() {
    val blockedRecipient = harness.others[0]
    ZonaRosaDatabase.recipients.setProfileName(blockedRecipient, ProfileName.fromParts("Blocked", "Person"))
    ZonaRosaDatabase.recipients.setBlocked(blockedRecipient, true)

    val results: MutableList<RecipientId> = ZonaRosaDatabase.recipients.getZonaRosaContacts(RecipientTable.IncludeSelfMode.Exclude).use {
      val ids = mutableListOf<RecipientId>()
      while (it.moveToNext()) {
        ids.add(RecipientId.from(CursorUtil.requireLong(it, RecipientTable.ID)))
      }

      ids
    }

    assertNotEquals(0, results.size)
    assertFalse(blockedRecipient in results)
  }

  @Test
  fun givenABlockedRecipient_whenIQueryZonaRosaContacts_thenIDoNotExpectBlockedToBeReturned() {
    val blockedRecipient = harness.others[0]
    ZonaRosaDatabase.recipients.setProfileName(blockedRecipient, ProfileName.fromParts("Blocked", "Person"))
    ZonaRosaDatabase.recipients.setBlocked(blockedRecipient, true)

    val results = ZonaRosaDatabase.recipients.queryZonaRosaContacts(RecipientTable.ContactSearchQuery("Blocked", RecipientTable.IncludeSelfMode.Exclude))!!

    assertEquals(0, results.count)
  }

  @Test
  fun givenABlockedRecipient_whenIGetNonGroupContacts_thenIDoNotExpectBlockedToBeReturned() {
    val blockedRecipient = harness.others[0]
    ZonaRosaDatabase.recipients.setProfileName(blockedRecipient, ProfileName.fromParts("Blocked", "Person"))
    ZonaRosaDatabase.recipients.setBlocked(blockedRecipient, true)

    val results: MutableList<RecipientId> = ZonaRosaDatabase.recipients.getNonGroupContacts(RecipientTable.IncludeSelfMode.Exclude)?.use {
      val ids = mutableListOf<RecipientId>()
      while (it.moveToNext()) {
        ids.add(RecipientId.from(CursorUtil.requireLong(it, RecipientTable.ID)))
      }

      ids
    }!!

    assertNotEquals(0, results.size)
    assertFalse(blockedRecipient in results)
  }

  @Test
  fun givenARecipientWithPniAndAci_whenIMarkItUnregistered_thenIExpectItToBeSplit() {
    val mainId = ZonaRosaDatabase.recipients.getAndPossiblyMerge(ACI_A, PNI_A, E164_A)

    ZonaRosaDatabase.recipients.markUnregistered(mainId)

    val byAci: RecipientId = ZonaRosaDatabase.recipients.getByAci(ACI_A).get()

    val byE164: RecipientId = ZonaRosaDatabase.recipients.getByE164(E164_A).get()
    val byPni: RecipientId = ZonaRosaDatabase.recipients.getByPni(PNI_A).get()

    assertEquals(mainId, byAci)
    assertEquals(byE164, byPni)
    assertNotEquals(byAci, byE164)
  }

  @Test
  fun givenARecipientWithPniAndAci_whenISplitItForStorageSync_thenIExpectItToBeSplit() {
    val mainId = ZonaRosaDatabase.recipients.getAndPossiblyMerge(ACI_A, PNI_A, E164_A)
    val mainRecord = ZonaRosaDatabase.recipients.getRecord(mainId)

    ZonaRosaDatabase.recipients.splitForStorageSyncIfNecessary(mainRecord.aci!!)

    val byAci: RecipientId = ZonaRosaDatabase.recipients.getByAci(ACI_A).get()

    val byE164: RecipientId = ZonaRosaDatabase.recipients.getByE164(E164_A).get()
    val byPni: RecipientId = ZonaRosaDatabase.recipients.getByPni(PNI_A).get()

    assertEquals(mainId, byAci)
    assertEquals(byE164, byPni)
    assertNotEquals(byAci, byE164)
  }

  companion object {
    val ACI_A = ACI.from(UUID.fromString("aaaa0000-5a76-47fa-a98a-7e72c948a82e"))
    val PNI_A = PNI.from(UUID.fromString("aaaa1111-c960-4f6c-8385-671ad2ffb999"))
    const val E164_A = "+12222222222"
  }
}
