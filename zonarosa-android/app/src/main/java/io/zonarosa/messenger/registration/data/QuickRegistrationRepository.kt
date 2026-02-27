/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.registration.data

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okio.ByteString.Companion.toByteString
import io.zonarosa.core.util.Base64.decode
import io.zonarosa.core.util.isNotNullOrBlank
import io.zonarosa.core.util.logging.Log
import io.zonarosa.libzonarosa.protocol.InvalidKeyException
import io.zonarosa.libzonarosa.protocol.ecc.ECPublicKey
import io.zonarosa.registration.proto.RegistrationProvisionMessage
import io.zonarosa.messenger.backup.v2.MessageBackupTier
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.net.ZonaRosaNetwork
import io.zonarosa.service.api.NetworkResult
import io.zonarosa.service.api.provisioning.RestoreMethod
import java.io.IOException
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration.Companion.seconds

/**
 * Helpers for quickly re-registering on a new device with the old device.
 */
object QuickRegistrationRepository {
  private val TAG = Log.tag(QuickRegistrationRepository::class)

  private const val REREG_URI_HOST = "rereg"

  fun isValidReRegistrationQr(data: String): Boolean {
    val uri = Uri.parse(data)

    if (!uri.isHierarchical) {
      return false
    }

    val ephemeralId: String? = uri.getQueryParameter("uuid")
    val publicKeyEncoded: String? = uri.getQueryParameter("pub_key")
    return uri.host == REREG_URI_HOST && ephemeralId.isNotNullOrBlank() && publicKeyEncoded.isNotNullOrBlank()
  }

  /**
   * Send registration provisioning message to new device.
   */
  fun transferAccount(reRegisterUri: String, restoreMethodToken: String): TransferAccountResult {
    if (!isValidReRegistrationQr(reRegisterUri)) {
      Log.w(TAG, "Invalid quick re-register qr data")
      return TransferAccountResult.FAILED
    }

    val uri = Uri.parse(reRegisterUri)

    try {
      val ephemeralId: String? = uri.getQueryParameter("uuid")
      val publicKeyEncoded: String? = uri.getQueryParameter("pub_key")

      if (ephemeralId == null || publicKeyEncoded == null) {
        Log.w(TAG, "Invalid link data hasId: ${ephemeralId != null} hasKey: ${publicKeyEncoded != null}")
        return TransferAccountResult.FAILED
      }

      val publicKey = ECPublicKey(decode(publicKeyEncoded))

      ZonaRosaNetwork
        .provisioning
        .sendReRegisterDeviceProvisioningMessage(
          ephemeralId,
          publicKey,
          RegistrationProvisionMessage(
            e164 = ZonaRosaStore.account.requireE164(),
            aci = ZonaRosaStore.account.requireAci().toByteString(),
            accountEntropyPool = ZonaRosaStore.account.accountEntropyPool.value,
            pin = ZonaRosaStore.svr.pin,
            platform = RegistrationProvisionMessage.Platform.ANDROID,
            backupTimestampMs = ZonaRosaStore.backup.lastBackupTime.coerceAtLeast(0L).takeIf { it > 0 },
            tier = when (ZonaRosaStore.backup.backupTier) {
              MessageBackupTier.PAID -> RegistrationProvisionMessage.Tier.PAID
              MessageBackupTier.FREE -> RegistrationProvisionMessage.Tier.FREE
              null -> null
            },
            backupSizeBytes = if (ZonaRosaStore.backup.backupTier == MessageBackupTier.PAID) ZonaRosaDatabase.attachments.getPaidEstimatedArchiveMediaSize().takeIf { it > 0 } else null,
            restoreMethodToken = restoreMethodToken,
            aciIdentityKeyPublic = ZonaRosaStore.account.aciIdentityKey.publicKey.serialize().toByteString(),
            aciIdentityKeyPrivate = ZonaRosaStore.account.aciIdentityKey.privateKey.serialize().toByteString(),
            pniIdentityKeyPublic = ZonaRosaStore.account.pniIdentityKey.publicKey.serialize().toByteString(),
            pniIdentityKeyPrivate = ZonaRosaStore.account.pniIdentityKey.privateKey.serialize().toByteString(),
            backupVersion = ZonaRosaStore.backup.lastBackupProtoVersion
          )
        )
        .successOrThrow()

      Log.i(TAG, "Re-registration provisioning message sent")
    } catch (e: IOException) {
      Log.w(TAG, "Exception re-registering new device", e)
      return TransferAccountResult.FAILED
    } catch (e: InvalidKeyException) {
      Log.w(TAG, "Exception re-registering new device", e)
      return TransferAccountResult.FAILED
    }

    return TransferAccountResult.SUCCESS
  }

  /**
   * Sets the restore method enum for the old device to retrieve and update their UI with.
   */
  suspend fun setRestoreMethodForOldDevice(restoreMethod: RestoreMethod) {
    val restoreMethodToken = ZonaRosaStore.registration.restoreMethodToken

    if (restoreMethodToken != null) {
      withContext(Dispatchers.IO) {
        Log.d(TAG, "Setting restore method ***${restoreMethodToken.takeLast(4)}: $restoreMethod")
        var retries = 3
        var result: NetworkResult<Unit>? = null
        while (retries-- > 0 && result !is NetworkResult.Success) {
          Log.d(TAG, "Setting method, retries remaining: $retries")
          result = AppDependencies.registrationApi.setRestoreMethod(restoreMethodToken, restoreMethod)

          if (result !is NetworkResult.Success) {
            delay(1.seconds)
          }
        }

        if (result is NetworkResult.Success) {
          Log.i(TAG, "Restore method set successfully")
          ZonaRosaStore.registration.restoreMethodToken = null
        } else {
          Log.w(TAG, "Restore method set failed", result?.getCause())
        }
      }
    }
  }

  /**
   * Gets the restore method used by the new device to update UI with. This is a long polling operation.
   */
  suspend fun waitForRestoreMethodSelectionOnNewDevice(restoreMethodToken: String): RestoreMethod {
    var retries = 5
    var result: NetworkResult<RestoreMethod>? = null

    Log.d(TAG, "Waiting for restore method with token: ***${restoreMethodToken.takeLast(4)}")
    while (retries-- > 0 && result !is NetworkResult.Success && coroutineContext.isActive) {
      Log.d(TAG, "Waiting, remaining tries: $retries")
      result = ZonaRosaNetwork.provisioning.waitForRestoreMethod(restoreMethodToken)
      Log.d(TAG, "Result: $result")
    }

    if (result is NetworkResult.Success) {
      Log.i(TAG, "Restore method selected on new device ${result.result}")
      return result.result
    } else {
      Log.w(TAG, "Failed to determine restore method, using DECLINE")
      return RestoreMethod.DECLINE
    }
  }

  enum class TransferAccountResult {
    SUCCESS,
    FAILED
  }
}
