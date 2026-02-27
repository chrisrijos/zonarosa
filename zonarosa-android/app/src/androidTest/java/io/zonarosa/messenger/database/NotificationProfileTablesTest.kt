package io.zonarosa.messenger.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import okio.ByteString.Companion.toByteString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import io.zonarosa.core.models.ServiceId.ACI
import io.zonarosa.core.util.UuidUtil
import io.zonarosa.core.util.deleteAll
import io.zonarosa.messenger.conversation.colors.AvatarColor
import io.zonarosa.messenger.notifications.profiles.NotificationProfile
import io.zonarosa.messenger.notifications.profiles.NotificationProfileId
import io.zonarosa.messenger.notifications.profiles.NotificationProfileSchedule
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.recipients.RecipientId
import io.zonarosa.messenger.storage.StorageSyncHelper
import io.zonarosa.messenger.testing.ZonaRosaActivityRule
import io.zonarosa.service.api.storage.ZonaRosaNotificationProfileRecord
import io.zonarosa.service.api.storage.StorageId
import java.time.DayOfWeek
import java.util.UUID
import io.zonarosa.service.internal.storage.protos.NotificationProfile as RemoteNotificationProfile
import io.zonarosa.service.internal.storage.protos.Recipient as RemoteRecipient

@RunWith(AndroidJUnit4::class)
class NotificationProfileTablesTest {

  @get:Rule
  val harness = ZonaRosaActivityRule()

  private lateinit var alice: RecipientId
  private lateinit var profile1: NotificationProfile

  @Before
  fun setUp() {
    alice = ZonaRosaDatabase.recipients.getOrInsertFromServiceId(ACI.from(UUID.randomUUID()))

    profile1 = NotificationProfile(
      id = 1,
      name = "profile1",
      emoji = "",
      createdAt = 1000L,
      schedule = NotificationProfileSchedule(id = 1),
      allowedMembers = setOf(alice),
      notificationProfileId = NotificationProfileId.generate(),
      deletedTimestampMs = 0,
      storageServiceId = StorageId.forNotificationProfile(byteArrayOf(1, 2, 3))
    )

    ZonaRosaDatabase.notificationProfiles.writableDatabase.deleteAll(NotificationProfileTables.NotificationProfileTable.TABLE_NAME)
    ZonaRosaDatabase.notificationProfiles.writableDatabase.deleteAll(NotificationProfileTables.NotificationProfileScheduleTable.TABLE_NAME)
    ZonaRosaDatabase.notificationProfiles.writableDatabase.deleteAll(NotificationProfileTables.NotificationProfileAllowedMembersTable.TABLE_NAME)
  }

  @Test
  fun givenARemoteProfile_whenIInsertLocally_thenIExpectAListWithThatProfile() {
    val remoteRecord =
      ZonaRosaNotificationProfileRecord(
        profile1.storageServiceId!!,
        RemoteNotificationProfile(
          id = UuidUtil.toByteArray(profile1.notificationProfileId.uuid).toByteString(),
          name = "profile1",
          emoji = "",
          color = profile1.color.colorInt(),
          createdAtMs = 1000L,
          allowedMembers = listOf(RemoteRecipient(RemoteRecipient.Contact(Recipient.resolved(alice).serviceId.get().toString()))),
          allowAllMentions = false,
          allowAllCalls = true,
          scheduleEnabled = false,
          scheduleStartTime = 900,
          scheduleEndTime = 1700,
          scheduleDaysEnabled = emptyList(),
          deletedAtTimestampMs = 0
        )
      )

    ZonaRosaDatabase.notificationProfiles.insertNotificationProfileFromStorageSync(remoteRecord)
    val actualProfiles = ZonaRosaDatabase.notificationProfiles.getProfiles()

    assertEquals(listOf(profile1), actualProfiles)
  }

  @Test
  fun givenAProfile_whenIDeleteIt_thenIExpectAnEmptyList() {
    val profile: NotificationProfile = ZonaRosaDatabase.notificationProfiles.createProfile(
      name = "Profile",
      emoji = "avatar",
      color = AvatarColor.A210,
      createdAt = 1000L
    ).profile

    ZonaRosaDatabase.notificationProfiles.deleteProfile(profile.id)

    assertThat(ZonaRosaDatabase.notificationProfiles.getProfiles()).isEmpty()
    assertThat(ZonaRosaDatabase.notificationProfiles.getProfile(profile.id))
  }

  @Test
  fun givenADeletedProfile_whenIGetIt_thenIExpectItToStillHaveASchedule() {
    val profile: NotificationProfile = ZonaRosaDatabase.notificationProfiles.createProfile(
      name = "Profile",
      emoji = "avatar",
      color = AvatarColor.A210,
      createdAt = 1000L
    ).profile

    ZonaRosaDatabase.notificationProfiles.deleteProfile(profile.id)

    val deletedProfile = ZonaRosaDatabase.notificationProfiles.getProfile(profile.id)!!
    assertThat(deletedProfile.schedule.enabled).isFalse()
    assertThat(deletedProfile.schedule.start).isEqualTo(900)
    assertThat(deletedProfile.schedule.end).isEqualTo(1700)
    assertThat(deletedProfile.schedule.daysEnabled, "Contains correct default days")
      .containsExactlyInAnyOrder(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
  }

  @Test
  fun givenNotificationProfiles_whenIUpdateTheirStorageSyncIds_thenIExpectAnUpdatedList() {
    ZonaRosaDatabase.notificationProfiles.createProfile(
      name = "Profile1",
      emoji = "avatar",
      color = AvatarColor.A210,
      createdAt = 1000L
    )
    ZonaRosaDatabase.notificationProfiles.createProfile(
      name = "Profile2",
      emoji = "avatar",
      color = AvatarColor.A210,
      createdAt = 2000L
    )

    val existingMap = ZonaRosaDatabase.notificationProfiles.getStorageSyncIdsMap()
    existingMap.forEach { (id, _) ->
      ZonaRosaDatabase.notificationProfiles.applyStorageIdUpdate(id, StorageId.forNotificationProfile(StorageSyncHelper.generateKey()))
    }
    val updatedMap = ZonaRosaDatabase.notificationProfiles.getStorageSyncIdsMap()

    existingMap.forEach { (id, storageId) ->
      assertNotEquals(storageId, updatedMap[id])
    }
  }

  @Test
  fun givenAProfileDeletedOver30Days_whenICleanUp_thenIExpectItToNotHaveAStorageId() {
    val remoteRecord =
      ZonaRosaNotificationProfileRecord(
        profile1.storageServiceId!!,
        RemoteNotificationProfile(
          id = UuidUtil.toByteArray(profile1.notificationProfileId.uuid).toByteString(),
          name = "profile1",
          emoji = "",
          color = profile1.color.colorInt(),
          createdAtMs = 1000L,
          deletedAtTimestampMs = 1000L
        )
      )

    ZonaRosaDatabase.notificationProfiles.insertNotificationProfileFromStorageSync(remoteRecord)
    ZonaRosaDatabase.notificationProfiles.removeStorageIdsFromOldDeletedProfiles(System.currentTimeMillis())
    assertThat(ZonaRosaDatabase.notificationProfiles.getStorageSyncIds()).isEmpty()
  }

  private val NotificationProfileTables.NotificationProfileChangeResult.profile: NotificationProfile
    get() = (this as NotificationProfileTables.NotificationProfileChangeResult.Success).notificationProfile
}
