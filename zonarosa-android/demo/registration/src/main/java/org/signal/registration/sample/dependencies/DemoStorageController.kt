/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.registration.sample.dependencies

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.zonarosa.core.models.AccountEntropyPool
import io.zonarosa.core.models.MasterKey
import io.zonarosa.core.util.Base64
import io.zonarosa.libzonarosa.protocol.IdentityKeyPair
import io.zonarosa.libzonarosa.protocol.ecc.ECKeyPair
import io.zonarosa.libzonarosa.protocol.kem.KEMKeyPair
import io.zonarosa.libzonarosa.protocol.kem.KEMKeyType
import io.zonarosa.libzonarosa.protocol.state.KyberPreKeyRecord
import io.zonarosa.libzonarosa.protocol.state.SignedPreKeyRecord
import io.zonarosa.libzonarosa.zkgroup.profiles.ProfileKey
import io.zonarosa.registration.KeyMaterial
import io.zonarosa.registration.NetworkController
import io.zonarosa.registration.NewRegistrationData
import io.zonarosa.registration.PreExistingRegistrationData
import io.zonarosa.registration.StorageController
import io.zonarosa.registration.sample.storage.RegistrationDatabase
import io.zonarosa.registration.sample.storage.RegistrationPreferences
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Implementation of [StorageController] that persists registration data using
 * SharedPreferences for simple key-value data and SQLite for prekeys.
 */
class DemoStorageController(context: Context) : StorageController {

  companion object {
    private const val MAX_SVR_CREDENTIALS = 10
  }

  private val db = RegistrationDatabase(context)

  override suspend fun generateAndStoreKeyMaterial(
    existingAccountEntropyPool: AccountEntropyPool?,
    existingAciIdentityKeyPair: IdentityKeyPair?,
    existingPniIdentityKeyPair: IdentityKeyPair?
  ): KeyMaterial = withContext(Dispatchers.IO) {
    val accountEntropyPool = existingAccountEntropyPool ?: AccountEntropyPool.generate()
    val aciIdentityKeyPair = existingAciIdentityKeyPair ?: IdentityKeyPair.generate()
    val pniIdentityKeyPair = existingPniIdentityKeyPair ?: IdentityKeyPair.generate()

    val aciSignedPreKeyId = generatePreKeyId()
    val pniSignedPreKeyId = generatePreKeyId()
    val aciKyberPreKeyId = generatePreKeyId()
    val pniKyberPreKeyId = generatePreKeyId()

    val timestamp = System.currentTimeMillis()

    val aciSignedPreKey = generateSignedPreKey(aciSignedPreKeyId, timestamp, aciIdentityKeyPair)
    val pniSignedPreKey = generateSignedPreKey(pniSignedPreKeyId, timestamp, pniIdentityKeyPair)
    val aciLastResortKyberPreKey = generateKyberPreKey(aciKyberPreKeyId, timestamp, aciIdentityKeyPair)
    val pniLastResortKyberPreKey = generateKyberPreKey(pniKyberPreKeyId, timestamp, pniIdentityKeyPair)

    val aciRegistrationId = generateRegistrationId()
    val pniRegistrationId = generateRegistrationId()
    val profileKey = generateProfileKey()
    val unidentifiedAccessKey = deriveUnidentifiedAccessKey(profileKey)
    val password = generatePassword()

    val keyMaterial = KeyMaterial(
      aciIdentityKeyPair = aciIdentityKeyPair,
      aciSignedPreKey = aciSignedPreKey,
      aciLastResortKyberPreKey = aciLastResortKyberPreKey,
      pniIdentityKeyPair = pniIdentityKeyPair,
      pniSignedPreKey = pniSignedPreKey,
      pniLastResortKyberPreKey = pniLastResortKyberPreKey,
      aciRegistrationId = aciRegistrationId,
      pniRegistrationId = pniRegistrationId,
      unidentifiedAccessKey = unidentifiedAccessKey,
      servicePassword = password,
      accountEntropyPool = accountEntropyPool
    )

    storeKeyMaterial(keyMaterial, profileKey)

    keyMaterial
  }

  override suspend fun saveNewRegistrationData(newRegistrationData: NewRegistrationData) = withContext(Dispatchers.IO) {
    RegistrationPreferences.saveRegistrationData(newRegistrationData)
  }

  override suspend fun getPreExistingRegistrationData(): PreExistingRegistrationData? = withContext(Dispatchers.IO) {
    RegistrationPreferences.getPreExistingRegistrationData()
  }

  override suspend fun clearAllData() = withContext(Dispatchers.IO) {
    RegistrationPreferences.clearAll()
    RegistrationPreferences.clearRestoredSvr2Credentials()
    db.clearAllPreKeys()
  }

  override suspend fun saveValidatedPinAndTemporaryMasterKey(pin: String, isAlphanumeric: Boolean, masterKey: MasterKey, registrationLockEnabled: Boolean) = withContext(Dispatchers.IO) {
    RegistrationPreferences.pin = pin
    RegistrationPreferences.pinAlphanumeric = isAlphanumeric
    RegistrationPreferences.temporaryMasterKey = masterKey
    RegistrationPreferences.registrationLockEnabled = registrationLockEnabled
  }

  override suspend fun getRestoredSvrCredentials(): List<NetworkController.SvrCredentials> = withContext(Dispatchers.IO) {
    RegistrationPreferences.restoredSvr2Credentials
  }

  override suspend fun appendSvrCredentials(credentials: List<NetworkController.SvrCredentials>) = withContext(Dispatchers.IO) {
    val existing = RegistrationPreferences.restoredSvr2Credentials
    val combined = (existing + credentials).distinctBy { it.username }.takeLast(MAX_SVR_CREDENTIALS)
    RegistrationPreferences.restoredSvr2Credentials = combined
  }

  override suspend fun saveNewlyCreatedPin(pin: String, isAlphanumeric: Boolean) {
    RegistrationPreferences.pin = pin
    RegistrationPreferences.pinAlphanumeric = isAlphanumeric
  }

  private fun storeKeyMaterial(keyMaterial: KeyMaterial, profileKey: ProfileKey) {
    // Clear existing data
    RegistrationPreferences.clearKeyMaterial()
    db.clearAllPreKeys()

    // Store in SharedPreferences
    RegistrationPreferences.aciIdentityKeyPair = keyMaterial.aciIdentityKeyPair
    RegistrationPreferences.pniIdentityKeyPair = keyMaterial.pniIdentityKeyPair
    RegistrationPreferences.aciRegistrationId = keyMaterial.aciRegistrationId
    RegistrationPreferences.pniRegistrationId = keyMaterial.pniRegistrationId
    RegistrationPreferences.profileKey = profileKey

    // Store prekeys in database
    db.signedPreKeys.insert(RegistrationDatabase.ACCOUNT_TYPE_ACI, keyMaterial.aciSignedPreKey)
    db.signedPreKeys.insert(RegistrationDatabase.ACCOUNT_TYPE_PNI, keyMaterial.pniSignedPreKey)
    db.kyberPreKeys.insert(RegistrationDatabase.ACCOUNT_TYPE_ACI, keyMaterial.aciLastResortKyberPreKey)
    db.kyberPreKeys.insert(RegistrationDatabase.ACCOUNT_TYPE_PNI, keyMaterial.pniLastResortKyberPreKey)
  }

  private fun generateSignedPreKey(id: Int, timestamp: Long, identityKeyPair: IdentityKeyPair): SignedPreKeyRecord {
    val keyPair = ECKeyPair.generate()
    val signature = identityKeyPair.privateKey.calculateSignature(keyPair.publicKey.serialize())
    return SignedPreKeyRecord(id, timestamp, keyPair, signature)
  }

  private fun generateKyberPreKey(id: Int, timestamp: Long, identityKeyPair: IdentityKeyPair): KyberPreKeyRecord {
    val kemKeyPair = KEMKeyPair.generate(KEMKeyType.KYBER_1024)
    val signature = identityKeyPair.privateKey.calculateSignature(kemKeyPair.publicKey.serialize())
    return KyberPreKeyRecord(id, timestamp, kemKeyPair, signature)
  }

  private fun generatePreKeyId(): Int {
    return SecureRandom().nextInt(Int.MAX_VALUE - 1) + 1
  }

  private fun generateRegistrationId(): Int {
    return SecureRandom().nextInt(16380) + 1
  }

  private fun generateProfileKey(): ProfileKey {
    val keyBytes = ByteArray(32)
    SecureRandom().nextBytes(keyBytes)
    return ProfileKey(keyBytes)
  }

  /**
   * Generates a password for basic auth during registration.
   * 18 random bytes, base64 encoded with padding.
   */
  private fun generatePassword(): String {
    val passwordBytes = ByteArray(18)
    SecureRandom().nextBytes(passwordBytes)
    return Base64.encodeWithPadding(passwordBytes)
  }

  /**
   * Derives the unidentified access key from a profile key.
   * This mirrors the logic in UnidentifiedAccess.deriveAccessKeyFrom().
   */
  private fun deriveUnidentifiedAccessKey(profileKey: ProfileKey): ByteArray {
    val nonce = ByteArray(12)
    val input = ByteArray(16)

    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(profileKey.serialize(), "AES"), GCMParameterSpec(128, nonce))

    val ciphertext = cipher.doFinal(input)
    return ciphertext.copyOf(16)
  }
}
