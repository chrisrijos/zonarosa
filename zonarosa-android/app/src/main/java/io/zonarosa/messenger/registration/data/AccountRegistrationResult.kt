/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.registration.data

import io.zonarosa.core.models.MasterKey
import io.zonarosa.service.api.account.PreKeyCollection

data class AccountRegistrationResult(
  val uuid: String,
  val pni: String,
  val storageCapable: Boolean,
  val number: String,
  val masterKey: MasterKey?,
  val pin: String?,
  val aciPreKeyCollection: PreKeyCollection,
  val pniPreKeyCollection: PreKeyCollection,
  val reRegistration: Boolean
)
