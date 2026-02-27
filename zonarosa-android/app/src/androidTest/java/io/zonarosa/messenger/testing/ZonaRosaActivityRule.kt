package io.zonarosa.messenger.testing

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.rules.ExternalResource
import io.zonarosa.core.models.ServiceId.ACI
import io.zonarosa.core.util.Util
import io.zonarosa.libzonarosa.protocol.IdentityKey
import io.zonarosa.libzonarosa.protocol.IdentityKeyPair
import io.zonarosa.libzonarosa.protocol.ZonaRosaProtocolAddress
import io.zonarosa.messenger.ZonaRosaInstrumentationApplicationContext
import io.zonarosa.messenger.crypto.MasterSecretUtil
import io.zonarosa.messenger.crypto.ProfileKeyUtil
import io.zonarosa.messenger.database.IdentityTable
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.database.model.databaseprotos.RestoreDecisionState
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.keyvalue.NewAccount
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.profiles.ProfileName
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.recipients.RecipientId
import io.zonarosa.messenger.registration.data.AccountRegistrationResult
import io.zonarosa.messenger.registration.data.LocalRegistrationMetadataUtil
import io.zonarosa.messenger.registration.data.RegistrationData
import io.zonarosa.messenger.registration.data.RegistrationRepository
import io.zonarosa.messenger.registration.util.RegistrationUtil
import io.zonarosa.messenger.testing.GroupTestingUtils.asMember
import io.zonarosa.service.api.profiles.ZonaRosaServiceProfile
import io.zonarosa.service.api.push.ZonaRosaServiceAddress
import java.util.UUID

/**
 * Test rule to use that sets up the application in a mostly registered state. Enough so that most
 * activities should be launchable directly.
 *
 * To use: `@get:Rule val harness = ZonaRosaActivityRule()`
 */
class ZonaRosaActivityRule(private val othersCount: Int = 4, private val createGroup: Boolean = false) : ExternalResource() {

  val application: Application = AppDependencies.application
  private val TEST_E164 = "+15555550101"

  lateinit var context: Context
    private set
  lateinit var self: Recipient
    private set
  lateinit var others: List<RecipientId>
    private set
  lateinit var othersKeys: List<IdentityKeyPair>

  var group: GroupTestingUtils.TestGroupInfo? = null
    private set

  val inMemoryLogger: InMemoryLogger
    get() = (application as ZonaRosaInstrumentationApplicationContext).inMemoryLogger

  override fun before() {
    context = InstrumentationRegistry.getInstrumentation().targetContext
    self = setupSelf()

    val setupOthers = setupOthers()
    others = setupOthers.first
    othersKeys = setupOthers.second

    if (createGroup && others.size >= 2) {
      group = GroupTestingUtils.insertGroup(
        revision = 0,
        self.asMember(),
        others[0].asMember(),
        others[1].asMember()
      )
    }
  }

  private fun setupSelf(): Recipient {
    PreferenceManager.getDefaultSharedPreferences(application).edit().putBoolean("pref_prompted_push_registration", true).commit()
    val masterSecret = MasterSecretUtil.generateMasterSecret(application, MasterSecretUtil.UNENCRYPTED_PASSPHRASE)
    MasterSecretUtil.generateAsymmetricMasterSecret(application, masterSecret)
    val preferences: SharedPreferences = application.getSharedPreferences(MasterSecretUtil.PREFERENCES_NAME, 0)
    preferences.edit().putBoolean("passphrase_initialized", true).commit()

    ZonaRosaStore.account.generateAciIdentityKeyIfNecessary()
    ZonaRosaStore.account.generatePniIdentityKeyIfNecessary()

    runBlocking {
      val registrationData = RegistrationData(
        code = "123123",
        e164 = TEST_E164,
        password = Util.getSecret(18),
        registrationId = RegistrationRepository.getRegistrationId(),
        profileKey = RegistrationRepository.getProfileKey(TEST_E164),
        fcmToken = null,
        pniRegistrationId = RegistrationRepository.getPniRegistrationId(),
        recoveryPassword = "asdfasdfasdfasdf"
      )
      val remoteResult = AccountRegistrationResult(
        uuid = UUID.randomUUID().toString(),
        pni = UUID.randomUUID().toString(),
        storageCapable = false,
        number = TEST_E164,
        masterKey = null,
        pin = null,
        aciPreKeyCollection = RegistrationRepository.generateSignedAndLastResortPreKeys(ZonaRosaStore.account.aciIdentityKey, ZonaRosaStore.account.aciPreKeys),
        pniPreKeyCollection = RegistrationRepository.generateSignedAndLastResortPreKeys(ZonaRosaStore.account.aciIdentityKey, ZonaRosaStore.account.pniPreKeys),
        reRegistration = false
      )
      val localRegistrationData = LocalRegistrationMetadataUtil.createLocalRegistrationMetadata(ZonaRosaStore.account.aciIdentityKey, ZonaRosaStore.account.pniIdentityKey, registrationData, remoteResult, false)
      RegistrationRepository.registerAccountLocally(application, localRegistrationData)
    }

    ZonaRosaStore.svr.optOut()
    RegistrationUtil.maybeMarkRegistrationComplete()
    ZonaRosaDatabase.recipients.setProfileName(Recipient.self().id, ProfileName.fromParts("Tester", "McTesterson"))

    ZonaRosaStore.settings.isMessageNotificationsEnabled = false
    ZonaRosaStore.registration.restoreDecisionState = RestoreDecisionState.NewAccount

    return Recipient.self()
  }

  private fun setupOthers(): Pair<List<RecipientId>, List<IdentityKeyPair>> {
    val others = mutableListOf<RecipientId>()
    val othersKeys = mutableListOf<IdentityKeyPair>()

    if (othersCount !in 0 until 1000) {
      throw IllegalArgumentException("$othersCount must be between 0 and 1000")
    }

    for (i in 0 until othersCount) {
      val aci = ACI.from(UUID.randomUUID())
      val recipientId = RecipientId.from(ZonaRosaServiceAddress(aci, "+15555551%03d".format(i)))
      ZonaRosaDatabase.recipients.setProfileName(recipientId, ProfileName.fromParts("Buddy", "#$i"))
      ZonaRosaDatabase.recipients.setProfileKeyIfAbsent(recipientId, ProfileKeyUtil.createNew())
      ZonaRosaDatabase.recipients.setCapabilities(recipientId, ZonaRosaServiceProfile.Capabilities(true, true))
      ZonaRosaDatabase.recipients.setProfileSharing(recipientId, true)
      ZonaRosaDatabase.recipients.markRegistered(recipientId, aci)
      val otherIdentity = IdentityKeyPair.generate()
      AppDependencies.protocolStore.aci().saveIdentity(ZonaRosaProtocolAddress(aci.toString(), 1), otherIdentity.publicKey)
      others += recipientId
      othersKeys += otherIdentity
    }

    return others to othersKeys
  }

  inline fun <reified T : Activity> launchActivity(initIntent: Intent.() -> Unit = {}): ActivityScenario<T> {
    return androidx.test.core.app.launchActivity(Intent(context, T::class.java).apply(initIntent))
  }

  fun changeIdentityKey(recipient: Recipient, identityKey: IdentityKey = IdentityKeyPair.generate().publicKey) {
    AppDependencies.protocolStore.aci().saveIdentity(ZonaRosaProtocolAddress(recipient.requireServiceId().toString(), 0), identityKey)
  }

  fun getIdentity(recipient: Recipient): IdentityKey {
    return AppDependencies.protocolStore.aci().identities().getIdentity(ZonaRosaProtocolAddress(recipient.requireServiceId().toString(), 0))
  }

  fun setVerified(recipient: Recipient, status: IdentityTable.VerifiedStatus) {
    AppDependencies.protocolStore.aci().identities().setVerified(recipient.id, getIdentity(recipient), IdentityTable.VerifiedStatus.VERIFIED)
  }
}
