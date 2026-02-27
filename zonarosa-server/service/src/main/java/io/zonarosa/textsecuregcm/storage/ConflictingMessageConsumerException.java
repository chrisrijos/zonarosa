/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.storage;

import io.zonarosa.server.util.NoStackTraceException;

/// Indicates that more than one consumer is trying to read a specific message queue at the same time.
public class ConflictingMessageConsumerException extends NoStackTraceException {
}
