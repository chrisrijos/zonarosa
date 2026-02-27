package io.zonarosa.util

import okio.ByteString.Companion.toByteString
import io.zonarosa.core.models.ServiceId
import io.zonarosa.core.util.Base64
import io.zonarosa.core.util.toByteArray
import io.zonarosa.libzonarosa.metadata.certificate.CertificateValidator
import io.zonarosa.libzonarosa.metadata.certificate.SenderCertificate
import io.zonarosa.libzonarosa.metadata.certificate.ServerCertificate
import io.zonarosa.libzonarosa.protocol.SessionBuilder
import io.zonarosa.libzonarosa.protocol.ZonaRosaProtocolAddress
import io.zonarosa.libzonarosa.protocol.ecc.ECKeyPair
import io.zonarosa.libzonarosa.protocol.ecc.ECPublicKey
import io.zonarosa.libzonarosa.protocol.groups.GroupSessionBuilder
import io.zonarosa.libzonarosa.protocol.kem.KEMKeyPair
import io.zonarosa.libzonarosa.protocol.kem.KEMKeyType
import io.zonarosa.libzonarosa.protocol.message.SenderKeyDistributionMessage
import io.zonarosa.libzonarosa.protocol.state.KyberPreKeyRecord
import io.zonarosa.libzonarosa.protocol.state.PreKeyBundle
import io.zonarosa.libzonarosa.protocol.state.PreKeyRecord
import io.zonarosa.libzonarosa.protocol.state.SignedPreKeyRecord
import io.zonarosa.service.api.ZonaRosaServiceAccountDataStore
import io.zonarosa.service.api.ZonaRosaSessionLock
import io.zonarosa.service.api.crypto.ContentHint
import io.zonarosa.service.api.crypto.EnvelopeContent
import io.zonarosa.service.api.crypto.SealedSenderAccess
import io.zonarosa.service.api.crypto.ZonaRosaGroupSessionBuilder
import io.zonarosa.service.api.crypto.ZonaRosaServiceCipher
import io.zonarosa.service.api.crypto.UnidentifiedAccess
import io.zonarosa.service.api.push.DistributionId
import io.zonarosa.service.api.push.ZonaRosaServiceAddress
import io.zonarosa.service.internal.push.Content
import io.zonarosa.service.internal.push.DataMessage
import io.zonarosa.service.internal.push.Envelope
import io.zonarosa.service.internal.push.OutgoingPushMessage
import io.zonarosa.service.internal.util.Util
import java.util.Optional
import java.util.UUID
import java.util.concurrent.locks.ReentrantLock
import kotlin.random.Random

/**
 * An in-memory zonarosa client that can encrypt and decrypt messages.
 *
 * Has a single prekey bundle that can be used to initialize a session with another client.
 */
class ZonaRosaClient {
  companion object {
    private val trustRoot: ECKeyPair = ECKeyPair.generate()
  }

  private val lock = TestSessionLock()

  private val aci: ServiceId.ACI = ServiceId.ACI.from(UUID.randomUUID())

  private val store: ZonaRosaServiceAccountDataStore = InMemoryZonaRosaServiceAccountDataStore()

  private var prekeyIndex = 0

  private val unidentifiedAccessKey: ByteArray = Util.getSecretBytes(32)

  private val senderCertificate: SenderCertificate = createCertificateFor(
    trustRoot = trustRoot,
    uuid = aci.rawUuid,
    e164 = "+${Random.nextLong(1111111111L, 9999999999L)}",
    deviceId = 1,
    identityKey = store.identityKeyPair.publicKey.publicKey,
    expires = Long.MAX_VALUE
  )

  private val cipher = ZonaRosaServiceCipher(ZonaRosaServiceAddress(aci), 1, store, lock, CertificateValidator(trustRoot.publicKey))

  /**
   * Sets up sessions using the [to] client's [preKeyBundles]. Note that you can only initialize a client up to 1,000 times because that's how many prekeys we have.
   */
  fun initializeSession(to: ZonaRosaClient) {
    val address = ZonaRosaProtocolAddress(to.aci.toString(), 1)
    SessionBuilder(store, address).process(to.createPreKeyBundle())
  }

  fun initializedGroupSession(distributionId: DistributionId): SenderKeyDistributionMessage {
    val self = ZonaRosaProtocolAddress(aci.toString(), 1)
    return ZonaRosaGroupSessionBuilder(lock, GroupSessionBuilder(store)).create(self, distributionId.asUuid())
  }

  fun encryptUnsealedSender(to: ZonaRosaClient): Envelope {
    val sentTimestamp = System.currentTimeMillis()

    val content = Content(
      dataMessage = DataMessage(
        body = "Test Message",
        timestamp = sentTimestamp
      )
    )

    val outgoingPushMessage: OutgoingPushMessage = cipher.encrypt(
      ZonaRosaProtocolAddress(to.aci.toString(), 1),
      SealedSenderAccess.NONE,
      EnvelopeContent.encrypted(content, ContentHint.RESENDABLE, Optional.empty())
    )

    val encryptedContent: ByteArray = Base64.decode(outgoingPushMessage.content)
    val serviceGuid = UUID.randomUUID()

    return Envelope(
      sourceServiceId = aci.toString(),
      sourceDevice = 1,
      destinationServiceId = to.aci.toString(),
      timestamp = sentTimestamp,
      serverTimestamp = sentTimestamp,
      serverGuid = serviceGuid.toString(),
      type = Envelope.Type.fromValue(outgoingPushMessage.type),
      urgent = true,
      content = encryptedContent.toByteString(),
      sourceServiceIdBinary = aci.toByteString(),
      destinationServiceIdBinary = to.aci.toByteString(),
      serverGuidBinary = serviceGuid.toByteArray().toByteString()
    )
  }

  fun encryptSealedSender(to: ZonaRosaClient): Envelope {
    val sentTimestamp = System.currentTimeMillis()

    val content = Content(
      dataMessage = DataMessage(
        body = "Test Message",
        timestamp = sentTimestamp
      )
    )

    val outgoingPushMessage: OutgoingPushMessage = cipher.encrypt(
      ZonaRosaProtocolAddress(to.aci.toString(), 1),
      SealedSenderAccess.forIndividual(UnidentifiedAccess(to.unidentifiedAccessKey, senderCertificate.serialized, false)),
      EnvelopeContent.encrypted(content, ContentHint.RESENDABLE, Optional.empty())
    )

    val encryptedContent: ByteArray = Base64.decode(outgoingPushMessage.content)
    val serverGuid = UUID.randomUUID()

    return Envelope(
      sourceServiceId = aci.toString(),
      sourceDevice = 1,
      destinationServiceId = to.aci.toString(),
      timestamp = sentTimestamp,
      serverTimestamp = sentTimestamp,
      serverGuid = serverGuid.toString(),
      type = Envelope.Type.fromValue(outgoingPushMessage.type),
      urgent = true,
      content = encryptedContent.toByteString(),
      sourceServiceIdBinary = aci.toByteString(),
      destinationServiceIdBinary = to.aci.toByteString(),
      serverGuidBinary = serverGuid.toByteArray().toByteString()
    )
  }

  fun multiEncryptSealedSender(distributionId: DistributionId, others: List<ZonaRosaClient>, groupId: Optional<ByteArray>): ByteArray {
    val sentTimestamp = System.currentTimeMillis()

    val content = Content(
      dataMessage = DataMessage(
        body = "Test Message",
        timestamp = sentTimestamp
      )
    )
    val destinations = others.map { other ->
      ZonaRosaProtocolAddress(other.aci.toString(), 1)
    }
    val sessionMap = store.getAllAddressesWithActiveSessions(destinations.map { it.name })

    return cipher.encryptForGroup(distributionId, destinations, sessionMap, senderCertificate, content.encode(), ContentHint.DEFAULT, groupId)
  }

  fun decryptMessage(envelope: Envelope) {
    cipher.decrypt(envelope, System.currentTimeMillis())
  }

  private fun createPreKeyBundle(): PreKeyBundle {
    val prekeyId = prekeyIndex++
    val preKeyRecord = PreKeyRecord(prekeyId, ECKeyPair.generate())
    val signedPreKeyPair = ECKeyPair.generate()
    val signedPreKeySignature = store.identityKeyPair.privateKey.calculateSignature(signedPreKeyPair.publicKey.serialize())

    val kyberPair = KEMKeyPair.generate(KEMKeyType.KYBER_1024)
    val kyberPreKeyRecord = KyberPreKeyRecord(prekeyId, System.currentTimeMillis() - 10, kyberPair, store.identityKeyPair.privateKey.calculateSignature(kyberPair.publicKey.serialize()))

    store.storePreKey(prekeyId, preKeyRecord)
    store.storeSignedPreKey(prekeyId, SignedPreKeyRecord(prekeyId, System.currentTimeMillis(), signedPreKeyPair, signedPreKeySignature))

    store.storeKyberPreKey(prekeyId, kyberPreKeyRecord)

    return PreKeyBundle(
      registrationId = prekeyId,
      deviceId = 1,
      preKeyId = prekeyId,
      preKeyPublic = preKeyRecord.keyPair.publicKey,
      signedPreKeyId = prekeyId,
      signedPreKeyPublic = signedPreKeyPair.publicKey,
      signedPreKeySignature = signedPreKeySignature,
      identityKey = store.identityKeyPair.publicKey,
      kyberPreKeyId = kyberPreKeyRecord.id,
      kyberPreKeyPublic = kyberPreKeyRecord.keyPair.publicKey,
      kyberPreKeySignature = kyberPreKeyRecord.signature
    )
  }
}

private fun createCertificateFor(trustRoot: ECKeyPair, uuid: UUID, e164: String, deviceId: Int, identityKey: ECPublicKey, expires: Long): SenderCertificate {
  val serverKey: ECKeyPair = ECKeyPair.generate()
  val serverCertificate = ServerCertificate(trustRoot.privateKey, 1, serverKey.publicKey)
  return serverCertificate.issue(serverKey.privateKey, uuid.toString(), Optional.of(e164), deviceId, identityKey, expires)
}

private class TestSessionLock : ZonaRosaSessionLock {
  val lock = ReentrantLock()

  override fun acquire(): ZonaRosaSessionLock.Lock {
    lock.lock()
    return ZonaRosaSessionLock.Lock { lock.unlock() }
  }
}
