/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.api.crypto

import io.zonarosa.core.models.backup.MediaId
import io.zonarosa.core.models.backup.MediaRootBackupKey.MediaKeyMaterial
import io.zonarosa.service.internal.util.Util

object AttachmentCipherTestHelper {

  /**
   * Needed to workaround this bug:
   * https://youtrack.jetbrains.com/issue/KT-60205/Java-class-has-private-access-in-class-constructor-with-inlinevalue-parameter
   */
  @JvmStatic
  fun createMediaKeyMaterial(combinedKey: ByteArray): MediaKeyMaterial {
    val parts = Util.split(combinedKey, 32, 32)

    return MediaKeyMaterial(
      id = MediaId(Util.getSecretBytes(15)),
      macKey = parts[1],
      aesKey = parts[0]
    )
  }
}
