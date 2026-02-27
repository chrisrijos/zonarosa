//
// Copyright 2024 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.zkgroup.backups;

public enum BackupCredentialType {
  // This must match the Rust version of the enum.
  MESSAGES(1),
  MEDIA(2);

  private final int value;

  BackupCredentialType(int value) {
    this.value = value;
  }

  int getValue() {
    return this.value;
  }

  public static BackupCredentialType fromValue(int value) {
    // A linear scan is simpler than a hash lookup for a set of values this small.
    for (final var credentialType : BackupCredentialType.values()) {
      if (credentialType.getValue() == value) {
        return credentialType;
      }
    }
    throw new IllegalArgumentException("Invalid backup credential type: " + value);
  }
}
