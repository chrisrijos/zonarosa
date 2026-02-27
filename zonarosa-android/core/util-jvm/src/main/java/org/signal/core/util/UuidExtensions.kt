/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.core.util

import java.util.UUID

fun UUID.toByteArray(): ByteArray = UuidUtil.toByteArray(this)
