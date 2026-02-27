/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.service.api.kbs

import io.zonarosa.core.models.MasterKey
import io.zonarosa.libzonarosa.svr2.Pin
import io.zonarosa.libzonarosa.svr2.PinHash
import io.zonarosa.service.api.crypto.HmacSIV
import io.zonarosa.service.api.crypto.InvalidCiphertextException
import java.nio.charset.StandardCharsets
import java.text.Normalizer

object PinHashUtil {

  /**
   * Takes a user-provided (i.e. non-normalized) PIN, normalizes it, and generates a [PinHash].
   */
  @JvmStatic
  fun hashPin(pin: String, salt: ByteArray): PinHash {
    return PinHash.svr1(normalize(pin), salt)
  }

  /**
   * Takes a user-provided (i.e. non-normalized) PIN, normalizes it, and generates a hash that is suitable for storing on-device
   * for the purpose of checking PIN reminder correctness. Use in combination with [verifyLocalPinHash].
   */
  @JvmStatic
  fun localPinHash(pin: String): String {
    return Pin.localHash(normalize(pin))
  }

  /**
   * Takes a user-provided (i.e. non-normalized) PIN, normalizes it, checks to see if it matches a hash that was generated with [localPinHash].
   */
  @JvmStatic
  fun verifyLocalPinHash(localPinHash: String, pin: String): Boolean {
    return Pin.verifyLocalHash(localPinHash, normalize(pin))
  }

  /**
   * Creates a new [KbsData] to store on KBS.
   */
  @JvmStatic
  fun createNewKbsData(pinHash: PinHash, masterKey: MasterKey): KbsData {
    val ivc = HmacSIV.encrypt(pinHash.encryptionKey(), masterKey.serialize())
    return KbsData(masterKey, pinHash.accessKey(), ivc)
  }

  /**
   * Takes 48 byte IVC from KBS and returns full [KbsData].
   */
  @JvmStatic
  @Throws(InvalidCiphertextException::class)
  fun decryptSvrDataIVCipherText(pinHash: PinHash, ivc: ByteArray?): KbsData {
    val masterKey = HmacSIV.decrypt(pinHash.encryptionKey(), ivc)
    return KbsData(MasterKey(masterKey), pinHash.accessKey(), ivc)
  }

  /**
   * Takes a user-input PIN string and normalizes it to a standard character set.
   */
  @JvmStatic
  fun normalizeToString(pin: String): String {
    var normalizedPin = pin.trim()

    if (PinString.allNumeric(normalizedPin)) {
      normalizedPin = PinString.toArabic(normalizedPin)
    }

    return Normalizer.normalize(normalizedPin, Normalizer.Form.NFKD)
  }

  /**
   * Takes a user-input PIN string and normalizes it to a standard character set.
   */
  @JvmStatic
  fun normalize(pin: String): ByteArray {
    return normalizeToString(pin).toByteArray(StandardCharsets.UTF_8)
  }
}
