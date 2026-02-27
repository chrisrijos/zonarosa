/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.backup;

import java.util.Map;

public record BackupUploadDescriptor(
    int cdn,
    String key,
    Map<String, String> headers,
    String signedUploadLocation) {}
