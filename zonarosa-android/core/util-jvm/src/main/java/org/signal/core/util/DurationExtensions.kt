/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.core.util

import kotlin.time.Duration
import kotlin.time.DurationUnit

fun Duration.inRoundedMilliseconds(places: Int = 2) = this.toDouble(DurationUnit.MILLISECONDS).roundedString(places)
fun Duration.inRoundedMinutes(places: Int = 2) = this.toDouble(DurationUnit.MINUTES).roundedString(places)
fun Duration.inRoundedHours(places: Int = 2) = this.toDouble(DurationUnit.HOURS).roundedString(places)
fun Duration.inRoundedDays(places: Int = 2) = this.toDouble(DurationUnit.DAYS).roundedString(places)
