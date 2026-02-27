/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.internal.push

import com.fasterxml.jackson.annotation.JsonCreator
import io.zonarosa.core.util.Base64
import io.zonarosa.libzonarosa.zkgroup.calllinks.CreateCallLinkCredentialRequest

/**
 * Request body to create a call link credential response.
 */
data class CreateCallLinkAuthRequest @JsonCreator constructor(
  val createCallLinkCredentialRequest: String
) {
  companion object {
    @JvmStatic
    fun create(createCallLinkCredentialRequest: CreateCallLinkCredentialRequest): CreateCallLinkAuthRequest {
      return CreateCallLinkAuthRequest(
        Base64.encodeWithPadding(createCallLinkCredentialRequest.serialize())
      )
    }
  }
}
