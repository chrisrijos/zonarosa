/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.core.util.logging

/**
 * A logger that does nothing.
 */
class NoopLogger : Log.Logger() {
  override fun v(tag: String, message: String?, t: Throwable?, keepLonger: Boolean) = Unit
  override fun d(tag: String, message: String?, t: Throwable?, keepLonger: Boolean) = Unit
  override fun i(tag: String, message: String?, t: Throwable?, keepLonger: Boolean) = Unit
  override fun w(tag: String, message: String?, t: Throwable?, keepLonger: Boolean) = Unit
  override fun e(tag: String, message: String?, t: Throwable?, keepLonger: Boolean) = Unit
  override fun flush() = Unit
}
