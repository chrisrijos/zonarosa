/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.registration.data

import android.content.Context
import android.util.Base64
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.recipients.Recipient
import java.io.File

/**
 * Exports current account registration credentials to a JSON file
 * that can be used with quickstart builds.
 */
object QuickstartCredentialExporter {

  private val TAG = Log.tag(QuickstartCredentialExporter::class.java)

  private val json = Json { prettyPrint = true }

  fun export(context: Context): File {
    val aci = ZonaRosaStore.account.requireAci()
    val pni = ZonaRosaStore.account.requirePni()
    val e164 = ZonaRosaStore.account.requireE164()
    val servicePassword = ZonaRosaStore.account.servicePassword ?: error("No service password")

    val aciIdentityKeyPair = ZonaRosaStore.account.aciIdentityKey
    val pniIdentityKeyPair = ZonaRosaStore.account.pniIdentityKey

    val aciSignedPreKey = AppDependencies.protocolStore.aci().loadSignedPreKey(ZonaRosaStore.account.aciPreKeys.activeSignedPreKeyId)
    val aciLastResortKyberPreKey = AppDependencies.protocolStore.aci().loadKyberPreKey(ZonaRosaStore.account.aciPreKeys.lastResortKyberPreKeyId)
    val pniSignedPreKey = AppDependencies.protocolStore.pni().loadSignedPreKey(ZonaRosaStore.account.pniPreKeys.activeSignedPreKeyId)
    val pniLastResortKyberPreKey = AppDependencies.protocolStore.pni().loadKyberPreKey(ZonaRosaStore.account.pniPreKeys.lastResortKyberPreKeyId)

    val self = Recipient.self()
    val profileKey = self.profileKey ?: error("No profile key")
    val profileName = self.profileName

    val credentials = QuickstartCredentials(
      aci = aci.toString(),
      pni = pni.toString(),
      e164 = e164,
      servicePassword = servicePassword,
      aciIdentityKeyPair = Base64.encodeToString(aciIdentityKeyPair.serialize(), Base64.NO_WRAP),
      pniIdentityKeyPair = Base64.encodeToString(pniIdentityKeyPair.serialize(), Base64.NO_WRAP),
      aciSignedPreKey = Base64.encodeToString(aciSignedPreKey.serialize(), Base64.NO_WRAP),
      aciLastResortKyberPreKey = Base64.encodeToString(aciLastResortKyberPreKey.serialize(), Base64.NO_WRAP),
      pniSignedPreKey = Base64.encodeToString(pniSignedPreKey.serialize(), Base64.NO_WRAP),
      pniLastResortKyberPreKey = Base64.encodeToString(pniLastResortKyberPreKey.serialize(), Base64.NO_WRAP),
      profileKey = Base64.encodeToString(profileKey, Base64.NO_WRAP),
      registrationId = ZonaRosaStore.account.registrationId,
      pniRegistrationId = ZonaRosaStore.account.pniRegistrationId,
      profileGivenName = profileName.givenName,
      profileFamilyName = profileName.familyName,
      accountEntropyPool = ZonaRosaStore.account.accountEntropyPool.value
    )

    val outputDir = context.getExternalFilesDir(null) ?: error("No external files directory")
    val outputFile = File(outputDir, "quickstart-credentials.json")
    outputFile.writeText(json.encodeToString(credentials))

    Log.i(TAG, "Exported quickstart credentials to ${outputFile.absolutePath}")
    return outputFile
  }
}
