/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

plugins {
  id("zonarosa-library")
  id("kotlin-parcelize")
  alias(libs.plugins.kotlinx.serialization)
}

android {
  namespace = "io.zonarosa.core.models"
}

dependencies {
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.jackson.core)
  implementation(libs.jackson.module.kotlin)
}
