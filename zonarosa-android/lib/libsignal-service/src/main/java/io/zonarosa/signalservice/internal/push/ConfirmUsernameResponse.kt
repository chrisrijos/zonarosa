package io.zonarosa.service.internal.push

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import io.zonarosa.service.internal.util.JsonUtil
import java.util.UUID

/** Response body for confirming a username reservation. */
class ConfirmUsernameResponse(
  @JsonProperty
  val usernameHash: String,

  @JsonProperty
  @JsonDeserialize(using = JsonUtil.UuidDeserializer::class)
  val usernameLinkHandle: UUID
)
