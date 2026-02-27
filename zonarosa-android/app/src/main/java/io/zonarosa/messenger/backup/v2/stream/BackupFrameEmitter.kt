/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.backup.v2.stream

import io.zonarosa.messenger.backup.v2.proto.Frame

/**
 * An interface that lets sub-processors emit [Frame]s as they export data.
 */
fun interface BackupFrameEmitter {
  fun emit(frame: Frame)
}
