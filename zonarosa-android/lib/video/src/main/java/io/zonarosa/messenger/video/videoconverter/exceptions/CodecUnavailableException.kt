/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.messenger.video.videoconverter.exceptions

import java.io.IOException

/**
 * Thrown when all available codec candidates have been exhausted (either they all
 * failed to configure/start, or they were all excluded due to mid-stream failures).
 * This indicates a device limitation, not a bug in the transcoding code.
 */
open class CodecUnavailableException(message: String, cause: Throwable? = null) : IOException(message, cause)
