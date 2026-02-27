//
// Copyright 2023 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.media;

/**
 * ZonaRosas that the given media input could not be parsed for some reason. Developer-readable
 * details are provided in the message.
 */
public class ParseException extends Exception {

  // This constructor is called by native code.
  @SuppressWarnings("unused")
  public ParseException(String msg) {
    super(msg);
  }

  // This constructor is called by native code.
  @SuppressWarnings("unused")
  public ParseException(Throwable t) {
    super(t);
  }
}
