/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.video.videoconverter.utils

import android.media.MediaCodecList
import android.media.MediaFormat
import io.zonarosa.core.util.isNotNullOrBlank

object DeviceCapabilities {
  @JvmStatic
  fun canEncodeHevc(): Boolean {
    val mediaCodecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
    val encoder = mediaCodecList.findEncoderForFormat(MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_HEVC, VideoConstants.VIDEO_LONG_EDGE_HD, VideoConstants.VIDEO_SHORT_EDGE_HD))
    return encoder.isNotNullOrBlank()
  }
}
