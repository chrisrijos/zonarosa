//
// Copyright 2023 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.metadata;

public class InvalidMetadataMessageException extends Exception {
  public InvalidMetadataMessageException(String s) {
    super(s);
  }

  public InvalidMetadataMessageException(Exception s) {
    super(s);
  }
}
