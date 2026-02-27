//
// Copyright 2023 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.messagebackup;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import io.zonarosa.libzonarosa.util.ResourceReader;

public class ComparableBackupTest {

  static final MessageBackup.Purpose BACKUP_PURPOSE = MessageBackup.Purpose.REMOTE_BACKUP;
  static final String CANONICAL_BACKUP_PROTO_NAME = "canonical-backup.binproto";
  static final String CANONICAL_BACKUP_STRING_NAME = "canonical-backup.expected.json";

  static InputStream getCanonicalBackupInputStream() {
    return ComparableBackupTest.class.getResourceAsStream(CANONICAL_BACKUP_PROTO_NAME);
  }

  @Test
  public void canonicalBackupString() throws IOException, ValidationError {
    final long length;
    try (InputStream input = getCanonicalBackupInputStream()) {
      length = ResourceReader.readAll(input).length;
    }

    ComparableBackup backup =
        ComparableBackup.readUnencrypted(BACKUP_PURPOSE, getCanonicalBackupInputStream(), length);
    assertArrayEquals(backup.getUnknownFieldMessages(), new String[] {});

    assertEquals(
        backup.getComparableString(),
        new String(
            ResourceReader.readAll(
                ComparableBackupTest.class.getResourceAsStream(CANONICAL_BACKUP_STRING_NAME)),
            StandardCharsets.UTF_8));
  }
}
