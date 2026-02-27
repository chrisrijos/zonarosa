//
// Copyright 2025 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.messagebackup;

import io.zonarosa.libzonarosa.zkgroup.InvalidInputException;
import io.zonarosa.libzonarosa.zkgroup.internal.ByteArray;

/**
 * A forward secrecy token used for deriving message backup keys.
 *
 * <p>This token is retrieved from the server when restoring a backup and is used together with the
 * backup key to derive the actual encryption keys for message backups.
 */
public class BackupForwardSecrecyToken extends ByteArray {
  public static final int SIZE = 32;

  public BackupForwardSecrecyToken(byte[] contents) throws InvalidInputException {
    super(contents, SIZE);
  }
}
