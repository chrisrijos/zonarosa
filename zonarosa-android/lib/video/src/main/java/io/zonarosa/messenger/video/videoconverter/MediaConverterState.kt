/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.video.videoconverter

data class MediaConverterState(val videoTrack: VideoTrackConverterState?, val audioTrack: AudioTrackConverterState?, val muxing: Boolean)

data class VideoTrackConverterState(val extractedCount: Long, val extractedDone: Boolean, val decodedCount: Long, val decodedDone: Boolean, val encodedCount: Long, val encodedDone: Boolean, val muxing: Boolean, val trackIndex: Int)

data class AudioTrackConverterState(val extractedCount: Long, val extractedDone: Boolean, val decodedCount: Long, val decodedDone: Boolean, val encodedCount: Long, val encodedDone: Boolean, val pendingBufferIndex: Int, val muxing: Boolean, val trackIndex: Int)
