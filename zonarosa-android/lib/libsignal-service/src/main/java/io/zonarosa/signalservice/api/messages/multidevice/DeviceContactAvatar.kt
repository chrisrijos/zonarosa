/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.api.messages.multidevice

import java.io.InputStream

/**
 * Data needed to sync a device contact avatar to/from other devices.
 */
data class DeviceContactAvatar(val inputStream: InputStream, val length: Long, val contentType: String)
