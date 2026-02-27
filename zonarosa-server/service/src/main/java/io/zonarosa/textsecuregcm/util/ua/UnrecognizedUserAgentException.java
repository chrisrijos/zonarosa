/*
 * Copyright 2013-2020 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.util.ua;

public class UnrecognizedUserAgentException extends Exception {

    public UnrecognizedUserAgentException() {
    }

    public UnrecognizedUserAgentException(final String message) {
        super(message);
    }

    public UnrecognizedUserAgentException(final Throwable cause) {
        super(cause);
    }
}
