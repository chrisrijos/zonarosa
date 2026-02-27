/*
 * Copyright 2013-2020 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.entities;

import java.util.List;

public record OutgoingMessageEntityList(List<OutgoingMessageEntity> messages, boolean more) {
}
