/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.telephony.hlrlookup;

import javax.annotation.Nullable;
import java.util.List;

record HlrLookupResponse(@Nullable List<HlrLookupResult> results,
                         @Nullable String error,
                         @Nullable String message) {
}
