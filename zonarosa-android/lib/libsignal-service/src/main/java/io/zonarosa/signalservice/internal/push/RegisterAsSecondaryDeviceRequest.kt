/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.internal.push

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.zonarosa.service.api.account.AccountAttributes
import io.zonarosa.service.api.push.SignedPreKeyEntity

class RegisterAsSecondaryDeviceRequest @JsonCreator constructor(
  @JsonProperty val verificationCode: String,
  @JsonProperty val accountAttributes: AccountAttributes,
  @JsonProperty val aciSignedPreKey: SignedPreKeyEntity,
  @JsonProperty val pniSignedPreKey: SignedPreKeyEntity,
  @JsonProperty val aciPqLastResortPreKey: KyberPreKeyEntity,
  @JsonProperty val pniPqLastResortPreKey: KyberPreKeyEntity,
  @JsonProperty val gcmToken: GcmRegistrationId?
)
