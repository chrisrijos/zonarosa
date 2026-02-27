/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.backup.v2.stream

import io.zonarosa.messenger.backup.v2.proto.BackupInfo
import io.zonarosa.messenger.backup.v2.proto.Frame

interface BackupExportWriter : AutoCloseable {
  fun write(header: BackupInfo)
  fun write(frame: Frame)
}
