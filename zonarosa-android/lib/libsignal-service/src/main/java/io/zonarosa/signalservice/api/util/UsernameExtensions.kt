/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.api.util

import io.zonarosa.libzonarosa.usernames.Username

val Username.nickname: String get() = username.split(Usernames.DELIMITER)[0]
val Username.discriminator: String get() = username.split(Usernames.DELIMITER)[1]
