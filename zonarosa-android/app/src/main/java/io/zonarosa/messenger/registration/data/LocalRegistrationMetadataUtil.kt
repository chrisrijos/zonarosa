/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.registration.data

import okio.ByteString.Companion.toByteString
import io.zonarosa.libzonarosa.protocol.IdentityKeyPair
import io.zonarosa.libzonarosa.protocol.state.KyberPreKeyRecord
import io.zonarosa.libzonarosa.protocol.state.SignedPreKeyRecord
import io.zonarosa.messenger.database.model.databaseprotos.LinkedDeviceInfo
import io.zonarosa.messenger.database.model.databaseprotos.LocalRegistrationMetadata
import io.zonarosa.service.api.account.PreKeyCollection

/**
 * Takes the two sources of registration data ([RegistrationData], [RegistrationRepository.AccountRegistrationResult])
 * and combines them into a proto-backed class [LocalRegistrationMetadata] so they can be serialized & stored.
 */
object LocalRegistrationMetadataUtil {
  fun createLocalRegistrationMetadata(
    localAciIdentityKeyPair: IdentityKeyPair,
    localPniIdentityKeyPair: IdentityKeyPair,
    registrationData: RegistrationData,
    remoteResult: AccountRegistrationResult,
    reglockEnabled: Boolean,
    linkedDeviceInfo: LinkedDeviceInfo? = null
  ): LocalRegistrationMetadata {
    return LocalRegistrationMetadata.Builder().apply {
      aciIdentityKeyPair = localAciIdentityKeyPair.serialize().toByteString()
      aciSignedPreKey = remoteResult.aciPreKeyCollection.signedPreKey.serialize().toByteString()
      aciLastRestoreKyberPreKey = remoteResult.aciPreKeyCollection.lastResortKyberPreKey.serialize().toByteString()
      pniIdentityKeyPair = localPniIdentityKeyPair.serialize().toByteString()
      pniSignedPreKey = remoteResult.pniPreKeyCollection.signedPreKey.serialize().toByteString()
      pniLastRestoreKyberPreKey = remoteResult.pniPreKeyCollection.lastResortKyberPreKey.serialize().toByteString()
      aci = remoteResult.uuid
      pni = remoteResult.pni
      hasPin = remoteResult.storageCapable
      remoteResult.pin?.let {
        pin = it
      }
      remoteResult.masterKey?.serialize()?.toByteString()?.let {
        masterKey = it
      }
      e164 = registrationData.e164
      fcmEnabled = registrationData.isFcm
      profileKey = registrationData.profileKey.serialize().toByteString()
      servicePassword = registrationData.password
      this.reglockEnabled = reglockEnabled
      this.linkedDeviceInfo = linkedDeviceInfo
    }.build()
  }

  fun LocalRegistrationMetadata.getAciIdentityKeyPair(): IdentityKeyPair {
    return IdentityKeyPair(aciIdentityKeyPair.toByteArray())
  }

  fun LocalRegistrationMetadata.getPniIdentityKeyPair(): IdentityKeyPair {
    return IdentityKeyPair(pniIdentityKeyPair.toByteArray())
  }

  fun LocalRegistrationMetadata.getAciPreKeyCollection(): PreKeyCollection {
    return PreKeyCollection(
      getAciIdentityKeyPair().publicKey,
      SignedPreKeyRecord(aciSignedPreKey.toByteArray()),
      KyberPreKeyRecord(aciLastRestoreKyberPreKey.toByteArray())
    )
  }

  fun LocalRegistrationMetadata.getPniPreKeyCollection(): PreKeyCollection {
    return PreKeyCollection(
      getPniIdentityKeyPair().publicKey,
      SignedPreKeyRecord(pniSignedPreKey.toByteArray()),
      KyberPreKeyRecord(pniLastRestoreKyberPreKey.toByteArray())
    )
  }
}
