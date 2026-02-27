/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.verify

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import io.zonarosa.core.util.concurrent.ZonaRosaDispatchers
import io.zonarosa.core.util.concurrent.ZonaRosaExecutors
import io.zonarosa.core.util.logging.Log
import io.zonarosa.libzonarosa.protocol.IdentityKey
import io.zonarosa.libzonarosa.protocol.fingerprint.Fingerprint
import io.zonarosa.libzonarosa.protocol.fingerprint.NumericFingerprintGenerator
import io.zonarosa.messenger.crypto.ProfileKeyUtil
import io.zonarosa.messenger.crypto.ReentrantSessionLock
import io.zonarosa.messenger.database.IdentityTable
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobs.MultiDeviceVerifiedUpdateJob
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.recipients.LiveRecipient
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.recipients.RecipientId
import io.zonarosa.messenger.storage.StorageSyncHelper
import io.zonarosa.messenger.util.IdentityUtil

class VerifySafetyNumberViewModel(
  private val recipientId: RecipientId,
  private val localIdentity: IdentityKey,
  private val remoteIdentity: IdentityKey
) : ViewModel() {

  companion object {
    val TAG = Log.tag(VerifySafetyNumberViewModel::class.java)
  }

  val recipient: LiveRecipient = Recipient.live(recipientId)

  private val fingerprintLiveData = MutableLiveData<SafetyNumberFingerprint?>()
  private val automaticVerificationLiveData = MutableLiveData(AutomaticVerificationStatus.NONE)

  init {
    initializeFingerprints()
    checkAutomaticVerificationEligibility()
  }

  private fun checkAutomaticVerificationEligibility() {
    if (recipient.get().e164.isEmpty ||
      recipient.get().aci.isEmpty ||
      ProfileKeyUtil.profileKeyOrNull(recipient.get().profileKey) == null ||
      ZonaRosaStore.misc.hasKeyTransparencyFailure
    ) {
      automaticVerificationLiveData.postValue(AutomaticVerificationStatus.UNAVAILABLE_PERMANENT)
    }
  }

  fun verifyAutomatically(canRetry: Boolean = true) {
    viewModelScope.launch(ZonaRosaDispatchers.IO) {
      if (automaticVerificationLiveData.value == AutomaticVerificationStatus.UNAVAILABLE_PERMANENT || !isActive) {
        return@launch
      }

      automaticVerificationLiveData.postValue(AutomaticVerificationStatus.VERIFYING)

      when (val result = VerifySafetyNumberRepository.verifyAutomatically(recipient.get())) {
        VerifySafetyNumberRepository.VerifyResult.Success -> {
          automaticVerificationLiveData.postValue(AutomaticVerificationStatus.VERIFIED)
        }
        is VerifySafetyNumberRepository.VerifyResult.RetryableFailure -> {
          if (canRetry) {
            delay(result.duration.toMillis())
            verifyAutomatically(canRetry = false)
          } else {
            Log.i(TAG, "Got a retryable exception, but we already retried once. Ignoring.")
            automaticVerificationLiveData.postValue(AutomaticVerificationStatus.UNAVAILABLE_TEMPORARY)
          }
        }
        VerifySafetyNumberRepository.VerifyResult.CorruptedFailure -> {
          Log.w(TAG, "KT store was corrupted. Clearing everything and starting again.")
          ZonaRosaStore.account.distinguishedHead = null
          ZonaRosaDatabase.recipients.setKeyTransparencyData(recipient.get().requireAci(), null)
          if (canRetry) {
            verifyAutomatically(canRetry = false)
          } else {
            Log.i(TAG, "Store was corrupted and we can retry, but we already retried once. Ignoring.")
            automaticVerificationLiveData.postValue(AutomaticVerificationStatus.UNAVAILABLE_TEMPORARY)
          }
        }
        VerifySafetyNumberRepository.VerifyResult.UnretryableFailure -> {
          automaticVerificationLiveData.postValue(AutomaticVerificationStatus.UNAVAILABLE_TEMPORARY)
        }
      }
    }
  }

  private fun initializeFingerprints() {
    ZonaRosaExecutors.UNBOUNDED.execute {
      val resolved = recipient.resolve()

      val generator = NumericFingerprintGenerator(5200)

      var aciFingerprint: SafetyNumberFingerprint? = null

      if (resolved.aci.isPresent) {
        val localIdentifier = ZonaRosaStore.account.requireAci().toByteArray()
        val remoteIdentifier = resolved.requireAci().toByteArray()
        val version = 2
        aciFingerprint = SafetyNumberFingerprint(version, localIdentifier, localIdentity, remoteIdentifier, remoteIdentity, generator.createFor(version, localIdentifier, localIdentity, remoteIdentifier, remoteIdentity))
      }

      if (aciFingerprint != null) {
        fingerprintLiveData.postValue(aciFingerprint)
      }
    }
  }

  fun getFingerprint(): LiveData<SafetyNumberFingerprint?> {
    return fingerprintLiveData
  }

  fun getAutomaticVerification(): LiveData<AutomaticVerificationStatus> {
    return automaticVerificationLiveData
  }

  fun updateSafetyNumberVerification(verified: Boolean) {
    val recipientId: RecipientId = recipientId
    val context: Context = AppDependencies.application

    ZonaRosaExecutors.BOUNDED.execute {
      ReentrantSessionLock.INSTANCE.acquire().use { _ ->
        if (verified) {
          Log.i(TAG, "Saving identity: $recipientId")
          AppDependencies.protocolStore.aci().identities()
            .saveIdentityWithoutSideEffects(
              recipientId,
              recipient.resolve().requireAci(),
              remoteIdentity,
              IdentityTable.VerifiedStatus.VERIFIED,
              false,
              System.currentTimeMillis(),
              true
            )
        } else {
          AppDependencies.protocolStore.aci().identities().setVerified(recipientId, remoteIdentity, IdentityTable.VerifiedStatus.DEFAULT)
        }
        AppDependencies.jobManager
          .add(
            MultiDeviceVerifiedUpdateJob(
              recipientId,
              remoteIdentity,
              if (verified) IdentityTable.VerifiedStatus.VERIFIED else IdentityTable.VerifiedStatus.DEFAULT
            )
          )
        StorageSyncHelper.scheduleSyncForDataChange()
        IdentityUtil.markIdentityVerified(context, recipient.get(), verified, false)
      }
    }
  }

  class Factory(
    private val recipientId: RecipientId,
    private val localIdentity: IdentityKey,
    private val remoteIdentity: IdentityKey
  ) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return modelClass.cast(VerifySafetyNumberViewModel(recipientId, localIdentity, remoteIdentity))!!
    }
  }
}

data class SafetyNumberFingerprint(
  val version: Int = 0,
  val localStableIdentifier: ByteArray?,
  val localIdentityKey: IdentityKey? = null,
  val remoteStableIdentifier: ByteArray?,
  val remoteIdentityKey: IdentityKey? = null,
  val fingerprint: Fingerprint
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as SafetyNumberFingerprint

    if (version != other.version) return false
    if (localStableIdentifier != null) {
      if (other.localStableIdentifier == null) return false
      if (!localStableIdentifier.contentEquals(other.localStableIdentifier)) return false
    } else if (other.localStableIdentifier != null) {
      return false
    }
    if (localIdentityKey != other.localIdentityKey) return false
    if (remoteStableIdentifier != null) {
      if (other.remoteStableIdentifier == null) return false
      if (!remoteStableIdentifier.contentEquals(other.remoteStableIdentifier)) return false
    } else if (other.remoteStableIdentifier != null) {
      return false
    }
    if (remoteIdentityKey != other.remoteIdentityKey) return false
    if (fingerprint != other.fingerprint) return false

    return true
  }

  override fun hashCode(): Int {
    var result = version
    result = 31 * result + (localStableIdentifier?.contentHashCode() ?: 0)
    result = 31 * result + (localIdentityKey?.hashCode() ?: 0)
    result = 31 * result + (remoteStableIdentifier?.contentHashCode() ?: 0)
    result = 31 * result + (remoteIdentityKey?.hashCode() ?: 0)
    result = 31 * result + fingerprint.hashCode()
    return result
  }
}

enum class AutomaticVerificationStatus {
  NONE,
  VERIFYING,
  UNAVAILABLE_PERMANENT,
  UNAVAILABLE_TEMPORARY,
  VERIFIED
}
