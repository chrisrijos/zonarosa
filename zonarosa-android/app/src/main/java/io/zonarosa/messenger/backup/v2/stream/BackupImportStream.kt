/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.backup.v2.stream

import io.zonarosa.messenger.backup.v2.proto.Frame

interface BackupImportStream {
  fun read(): Frame?
}
