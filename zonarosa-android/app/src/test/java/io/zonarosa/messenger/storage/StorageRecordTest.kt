/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.storage

import okio.ByteString
import okio.ByteString.Companion.toByteString
import org.junit.Assert.assertEquals
import org.junit.Test
import io.zonarosa.core.util.Util
import io.zonarosa.service.api.storage.ZonaRosaAccountRecord
import io.zonarosa.service.api.storage.ZonaRosaContactRecord
import io.zonarosa.service.api.storage.StorageId
import io.zonarosa.service.internal.storage.protos.AccountRecord
import io.zonarosa.service.internal.storage.protos.ContactRecord

class StorageRecordTest {

  @Test
  fun `describeDiff - general test`() {
    val a = ZonaRosaAccountRecord(
      StorageId.forAccount(Util.getSecretBytes(16)),
      AccountRecord(
        profileKey = ByteString.EMPTY,
        givenName = "First",
        familyName = "Last"
      )
    )

    val b = ZonaRosaAccountRecord(
      StorageId.forAccount(Util.getSecretBytes(16)),
      AccountRecord(
        profileKey = Util.getSecretBytes(16).toByteString(),
        givenName = "First",
        familyName = "LastB"
      )
    )

    assertEquals("Some fields differ: familyName, id, profileKey", a.describeDiff(b))
  }

  @Test
  fun `describeDiff - different class`() {
    val a = ZonaRosaAccountRecord(
      StorageId.forAccount(Util.getSecretBytes(16)),
      AccountRecord()
    )

    val b = ZonaRosaContactRecord(
      StorageId.forAccount(Util.getSecretBytes(16)),
      ContactRecord()
    )

    assertEquals("Classes are different!", a.describeDiff(b))
  }
}
