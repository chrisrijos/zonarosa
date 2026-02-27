package io.zonarosa.messenger.testing

import io.zonarosa.core.models.ServiceId
import io.zonarosa.libzonarosa.protocol.IdentityKeyPair
import io.zonarosa.libzonarosa.protocol.ecc.ECKeyPair
import io.zonarosa.libzonarosa.protocol.state.PreKeyRecord
import io.zonarosa.libzonarosa.protocol.util.KeyHelper
import io.zonarosa.libzonarosa.protocol.util.Medium
import io.zonarosa.messenger.crypto.PreKeyUtil
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.service.api.messages.multidevice.DeviceInfo
import io.zonarosa.service.api.push.SignedPreKeyEntity
import io.zonarosa.service.internal.push.AuthCredentials
import io.zonarosa.service.internal.push.DeviceInfoList
import io.zonarosa.service.internal.push.PreKeyEntity
import io.zonarosa.service.internal.push.PreKeyResponse
import io.zonarosa.service.internal.push.PreKeyResponseItem
import io.zonarosa.service.internal.push.PushServiceSocket
import io.zonarosa.service.internal.push.RegistrationSessionMetadataJson
import io.zonarosa.service.internal.push.SenderCertificate
import io.zonarosa.service.internal.push.VerifyAccountResponse
import io.zonarosa.service.internal.push.WhoAmIResponse
import java.security.SecureRandom

/**
 * Warehouse of reusable test data and mock configurations.
 */
object MockProvider {

  val senderCertificate = SenderCertificate().apply { certificate = ByteArray(0) }

  val lockedFailure = PushServiceSocket.RegistrationLockFailure().apply {
    svr1Credentials = AuthCredentials.create("username", "password")
    svr2Credentials = AuthCredentials.create("username", "password")
  }

  val primaryOnlyDeviceList = DeviceInfoList().apply {
    devices = listOf(
      DeviceInfo().apply {
        id = 1
      }
    )
  }

  val sessionMetadataJson = RegistrationSessionMetadataJson(
    id = "asdfasdfasdfasdf",
    nextCall = null,
    nextSms = null,
    nextVerificationAttempt = null,
    allowedToRequestCode = true,
    requestedInformation = emptyList(),
    verified = true
  )

  fun createVerifyAccountResponse(aci: ServiceId, newPni: ServiceId): VerifyAccountResponse {
    return VerifyAccountResponse().apply {
      uuid = aci.toString()
      pni = newPni.toString()
      storageCapable = false
    }
  }

  fun createWhoAmIResponse(aci: ServiceId, pni: ServiceId, e164: String): WhoAmIResponse {
    return WhoAmIResponse(
      aci = aci.toString(),
      pni = pni.toString(),
      number = e164
    )
  }

  fun createPreKeyResponse(identity: IdentityKeyPair = ZonaRosaStore.account.aciIdentityKey, deviceId: Int): PreKeyResponse {
    val signedPreKeyRecord = PreKeyUtil.generateSignedPreKey(SecureRandom().nextInt(Medium.MAX_VALUE), identity.privateKey)
    val oneTimePreKey = PreKeyRecord(SecureRandom().nextInt(Medium.MAX_VALUE), ECKeyPair.generate())

    val device = PreKeyResponseItem().apply {
      this.deviceId = deviceId
      registrationId = KeyHelper.generateRegistrationId(false)
      signedPreKey = SignedPreKeyEntity(signedPreKeyRecord.id, signedPreKeyRecord.keyPair.publicKey, signedPreKeyRecord.signature)
      preKey = PreKeyEntity(oneTimePreKey.id, oneTimePreKey.keyPair.publicKey)
    }

    return PreKeyResponse().apply {
      identityKey = identity.publicKey
      devices = listOf(device)
    }
  }
}
