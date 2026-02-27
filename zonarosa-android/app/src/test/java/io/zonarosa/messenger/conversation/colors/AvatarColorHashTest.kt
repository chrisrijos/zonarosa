/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.conversation.colors

import org.junit.Assert.assertEquals
import org.junit.Test
import io.zonarosa.core.models.ServiceId.ACI
import io.zonarosa.core.models.ServiceId.PNI
import io.zonarosa.core.util.Base64
import io.zonarosa.messenger.groups.GroupId

class AvatarColorHashTest {

  @Test
  fun `hash test vector - ACI`() {
    assertEquals(AvatarColor.A140, AvatarColorHash.forAddress(ACI.parseOrThrow("a025bf78-653e-44e0-beb9-deb14ba32487"), null))
  }

  @Test
  fun `hash test vector - PNI`() {
    assertEquals(AvatarColor.A200, AvatarColorHash.forAddress(PNI.parseOrThrow("11a175e3-fe31-4eda-87da-e0bf2a2e250b"), null))
  }

  @Test
  fun `hash test vector - E164`() {
    assertEquals(AvatarColor.A150, AvatarColorHash.forAddress(null, "+12135550124"))
  }

  @Test
  fun `hash test vector - GroupId`() {
    assertEquals(AvatarColor.A130, AvatarColorHash.forGroupId(GroupId.push(Base64.decode("BwJRIdomqOSOckHjnJsknNCibCZKJFt+RxLIpa9CWJ4="))))
  }
}
