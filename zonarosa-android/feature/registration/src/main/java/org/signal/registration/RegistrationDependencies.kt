/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.registration

import io.zonarosa.core.util.logging.Log
import io.zonarosa.registration.util.SensitiveLog

/**
 * Injection point for dependencies needed by this module.
 *
 * @param sensitiveLogger A logger for logging sensitive material. The intention is this would only be used in the demo app for testing + debugging, while
 *   the actual app would just pass null.
 */
class RegistrationDependencies(
  val networkController: NetworkController,
  val storageController: StorageController,
  val sensitiveLogger: Log.Logger?
) {
  companion object {
    lateinit var dependencies: RegistrationDependencies

    fun provide(registrationDependencies: RegistrationDependencies) {
      dependencies = registrationDependencies
      SensitiveLog.init(dependencies.sensitiveLogger)
    }

    fun get(): RegistrationDependencies = dependencies
  }
}
