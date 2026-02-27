/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.messenger.video.videoconverter.exceptions

/**
 * Thrown when no decoder on the device can properly decode HDR video content.
 * This is typically a device limitation, not a bug.
 */
class HdrDecoderUnavailableException(message: String, cause: Throwable?) : CodecUnavailableException(message, cause)
