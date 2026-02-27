//
// Copyright 2025 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.net;

/**
 * Exception thrown when the server explicitly invalidates and disconnects a connection.
 *
 * <p>Indicates that the server has invalidated our connection for some reason and explicitly
 * disconnected us. We are allowed try to reconnect in the future with the same credentials. If our
 * connection is invalidated after a call to the account deletion endpoint, the client is allowed to
 * assume that the account deletion request succeeded.
 */
public class ConnectionInvalidatedException extends ChatServiceException {
  public ConnectionInvalidatedException(String message) {
    super(message);
  }
}
