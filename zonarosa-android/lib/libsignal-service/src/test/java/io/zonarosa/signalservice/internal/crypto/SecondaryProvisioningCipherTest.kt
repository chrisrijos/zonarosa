/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.internal.crypto

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import okio.ByteString
import okio.ByteString.Companion.toByteString
import org.junit.Test
import io.zonarosa.core.util.UuidUtil
import io.zonarosa.core.util.toByteArray
import io.zonarosa.libzonarosa.protocol.IdentityKey
import io.zonarosa.libzonarosa.protocol.IdentityKeyPair
import io.zonarosa.libzonarosa.protocol.ecc.ECPrivateKey
import io.zonarosa.libzonarosa.zkgroup.profiles.ProfileKey
import io.zonarosa.service.internal.push.ProvisionEnvelope
import io.zonarosa.service.internal.push.ProvisionMessage
import io.zonarosa.service.internal.push.ProvisioningVersion
import java.util.UUID
import kotlin.random.Random

class SecondaryProvisioningCipherTest {
  @Test
  fun decrypt() {
    val provisioningCipher = SecondaryProvisioningCipher.generate(IdentityKeyPair.generate())

    val primaryIdentityKeyPair = IdentityKeyPair.generate()
    val primaryProfileKey = generateProfileKey()
    val primaryProvisioningCipher = PrimaryProvisioningCipher(provisioningCipher.secondaryDevicePublicKey.publicKey)
    val aci = UUID.randomUUID()

    val message = ProvisionMessage(
      aciIdentityKeyPublic = ByteString.of(*primaryIdentityKeyPair.publicKey.serialize()),
      aciIdentityKeyPrivate = ByteString.of(*primaryIdentityKeyPair.privateKey.serialize()),
      provisioningCode = "code",
      provisioningVersion = ProvisioningVersion.CURRENT.value,
      number = "+14045555555",
      aci = aci.toString(),
      profileKey = ByteString.of(*primaryProfileKey.serialize()),
      readReceipts = true,
      aciBinary = aci.toByteArray().toByteString()
    )

    val provisionMessage = ProvisionEnvelope.ADAPTER.decode(primaryProvisioningCipher.encrypt(message))

    val result = provisioningCipher.decrypt(provisionMessage)
    assertThat(result).isInstanceOf<SecondaryProvisioningCipher.ProvisioningDecryptResult.Success<ProvisionMessage>>()

    val success = result as SecondaryProvisioningCipher.ProvisioningDecryptResult.Success<ProvisionMessage>

    assertThat(message.aci).isEqualTo(UuidUtil.parseOrThrow(success.message.aci!!).toString())
    assertThat(message.number).isEqualTo(success.message.number)
    assertThat(primaryIdentityKeyPair.serialize()).isEqualTo(IdentityKeyPair(IdentityKey(success.message.aciIdentityKeyPublic!!.toByteArray()), ECPrivateKey(success.message.aciIdentityKeyPrivate!!.toByteArray())).serialize())
    assertThat(primaryProfileKey.serialize()).isEqualTo(ProfileKey(success.message.profileKey!!.toByteArray()).serialize())
    assertThat(message.readReceipts).isEqualTo(success.message.readReceipts == true)
    assertThat(message.userAgent).isEqualTo(success.message.userAgent)
    assertThat(message.provisioningCode).isEqualTo(success.message.provisioningCode!!)
    assertThat(message.provisioningVersion).isEqualTo(success.message.provisioningVersion!!)
    assertThat(message.aciBinary).isEqualTo(UuidUtil.parseOrThrow(success.message.aciBinary!!).toByteArray().toByteString())
  }

  companion object {
    fun generateProfileKey(): ProfileKey {
      return ProfileKey(Random.nextBytes(32))
    }
  }
}
