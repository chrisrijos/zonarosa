/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.controllers;

import java.util.List;
import io.zonarosa.server.auth.TurnToken;

public record GetCallingRelaysResponse(List<TurnToken> relays) {
}
