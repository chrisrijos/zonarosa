package io.zonarosa.messenger.groups

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import io.zonarosa.core.models.ServiceId.ACI
import io.zonarosa.core.models.ServiceId.PNI
import io.zonarosa.core.util.Hex
import io.zonarosa.core.util.ThreadUtil
import io.zonarosa.core.util.logging.Log
import io.zonarosa.libzonarosa.protocol.logging.ZonaRosaProtocolLogger
import io.zonarosa.libzonarosa.protocol.logging.ZonaRosaProtocolLoggerProvider
import io.zonarosa.libzonarosa.zkgroup.groups.GroupMasterKey
import io.zonarosa.libzonarosa.zkgroup.groups.GroupSecretParams
import io.zonarosa.storageservice.storage.protos.groups.GroupChangeResponse
import io.zonarosa.storageservice.storage.protos.groups.Member
import io.zonarosa.storageservice.storage.protos.groups.local.DecryptedGroup
import io.zonarosa.messenger.TestZkGroupServer
import io.zonarosa.messenger.database.GroupStateTestData
import io.zonarosa.messenger.database.GroupTable
import io.zonarosa.messenger.database.model.databaseprotos.member
import io.zonarosa.messenger.groups.v2.GroupCandidateHelper
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.logging.CustomZonaRosaProtocolLogger
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.testutil.MockAppDependenciesRule
import io.zonarosa.messenger.testutil.SystemOutLogger
import io.zonarosa.messenger.util.RemoteConfig
import io.zonarosa.service.api.groupsv2.ClientZkOperations
import io.zonarosa.service.api.groupsv2.GroupsV2Api
import io.zonarosa.service.api.groupsv2.GroupsV2Operations
import io.zonarosa.service.api.push.ServiceIds
import java.util.UUID

@Suppress("ClassName")
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, application = Application::class)
class GroupManagerV2Test_edit {
  companion object {
    val server: TestZkGroupServer = TestZkGroupServer()
    val masterKey: GroupMasterKey = GroupMasterKey(Hex.fromStringCondensed("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"))
    val groupSecretParams: GroupSecretParams = GroupSecretParams.deriveFromMasterKey(masterKey)
    val groupId: GroupId.V2 = GroupId.v2(masterKey)

    val selfAci: ACI = ACI.from(UUID.randomUUID())
    val selfPni: PNI = PNI.from(UUID.randomUUID())
    val serviceIds: ServiceIds = ServiceIds(selfAci, selfPni)
    val otherAci: ACI = ACI.from(UUID.randomUUID())
  }

  @get:Rule
  val appDependencies = MockAppDependenciesRule()

  private lateinit var groupTable: GroupTable
  private lateinit var groupsV2API: GroupsV2Api
  private lateinit var groupsV2Operations: GroupsV2Operations
  private lateinit var groupsV2Authorization: GroupsV2Authorization
  private lateinit var groupCandidateHelper: GroupCandidateHelper
  private lateinit var sendGroupUpdateHelper: GroupManagerV2.SendGroupUpdateHelper
  private lateinit var groupOperations: GroupsV2Operations.GroupOperations

  private lateinit var manager: GroupManagerV2

  @Suppress("UsePropertyAccessSyntax")
  @Before
  fun setUp() {
    mockkObject(RemoteConfig)
    mockkObject(ZonaRosaStore)
    every { RemoteConfig.internalUser } returns false

    ThreadUtil.enforceAssertions = false
    Log.initialize(SystemOutLogger())
    ZonaRosaProtocolLoggerProvider.setProvider(CustomZonaRosaProtocolLogger())
    ZonaRosaProtocolLoggerProvider.initializeLogging(ZonaRosaProtocolLogger.INFO)

    val clientZkOperations = ClientZkOperations(server.getServerPublicParams())

    groupTable = mockk()
    groupsV2API = mockk()
    groupsV2Operations = GroupsV2Operations(clientZkOperations, 1000)
    groupsV2Authorization = mockk(relaxed = true)
    groupCandidateHelper = mockk()
    sendGroupUpdateHelper = mockk()
    groupOperations = groupsV2Operations.forGroup(groupSecretParams)

    manager = GroupManagerV2(
      ApplicationProvider.getApplicationContext(),
      groupTable,
      groupsV2API,
      groupsV2Operations,
      groupsV2Authorization,
      serviceIds,
      groupCandidateHelper,
      sendGroupUpdateHelper
    )
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  private fun given(init: GroupStateTestData.() -> Unit) {
    val data = GroupStateTestData(masterKey, groupOperations)
    data.init()

    every { groupTable.getGroup(groupId) } returns data.groupRecord
    every { groupTable.requireGroup(groupId) } returns data.groupRecord.get()
    every { groupTable.update(any<GroupId.V2>(), any(), any()) } returns Unit
    every { sendGroupUpdateHelper.sendGroupUpdate(masterKey, any(), any(), any()) } returns GroupManagerV2.RecipientAndThread(Recipient.UNKNOWN, 1)
    every { groupsV2API.patchGroup(any(), any(), any()) } returns GroupChangeResponse(group_change = data.groupChange!!)
  }

  private fun editGroup(perform: GroupManagerV2.GroupEditor.() -> Unit) {
    manager.edit(groupId).use { it.perform() }
  }

  private fun then(then: (DecryptedGroup) -> Unit) {
    val decryptedGroupArg = slot<DecryptedGroup>()
    verify { groupTable.update(groupId, capture(decryptedGroupArg), any()) }
    then(decryptedGroupArg.captured)
  }

  @Test
  fun `when you are the only admin, and then leave the group, server upgrades all other members to administrators and lets you leave`() {
    given {
      localState(
        revision = 5,
        members = listOf(
          member(selfAci, role = Member.Role.ADMINISTRATOR),
          member(otherAci)
        )
      )
      groupChange(6) {
        source(selfAci)
        deleteMember(selfAci)
        modifyRole(otherAci, Member.Role.ADMINISTRATOR)
      }
    }

    editGroup {
      leaveGroup(true)
    }

    then { patchedGroup ->
      assertThat(patchedGroup.revision, "Revision updated by one").isEqualTo(6)
      assertThat(patchedGroup.members.find { it.aciBytes == selfAci.toByteString() }, "Self is no longer in the group").isNull()
      assertThat(patchedGroup.members.find { it.aciBytes == otherAci.toByteString() }?.role, "Other is now an admin in the group").isEqualTo(Member.Role.ADMINISTRATOR)
    }
  }
}
