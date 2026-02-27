/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.messenger.video.exceptions

import java.io.IOException

/**
 * Exception to denote when video processing has been unable to meet its output file size requirements.
 */
class VideoSizeException(message: String?) : IOException(message)
