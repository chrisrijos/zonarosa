/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.testing

@Retention(AnnotationRetention.RUNTIME)
annotation class ZonaRosaFlakyTest(val allowedAttempts: Int = 3)
