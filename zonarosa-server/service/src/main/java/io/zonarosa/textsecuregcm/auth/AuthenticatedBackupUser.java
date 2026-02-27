/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.auth;

import io.zonarosa.libzonarosa.zkgroup.backups.BackupCredentialType;
import io.zonarosa.libzonarosa.zkgroup.backups.BackupLevel;
import io.zonarosa.server.util.ua.UserAgent;
import javax.annotation.Nullable;

public record AuthenticatedBackupUser(
    byte[] backupId,
    BackupCredentialType credentialType,
    BackupLevel backupLevel,
    String backupDir,
    String mediaDir,
    @Nullable UserAgent userAgent) {
}
