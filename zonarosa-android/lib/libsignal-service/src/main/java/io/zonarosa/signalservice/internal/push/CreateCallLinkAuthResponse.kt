/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.internal.push

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.zonarosa.core.util.Base64
import io.zonarosa.libzonarosa.zkgroup.calllinks.CreateCallLinkCredentialResponse

/**
 * Response body for CreateCallLinkAuthResponse
 */
data class CreateCallLinkAuthResponse @JsonCreator constructor(
  @JsonProperty("credential") val credential: String
) {
  val createCallLinkCredentialResponse: CreateCallLinkCredentialResponse
    get() = CreateCallLinkCredentialResponse(Base64.decode(credential))
}
