//
// Copyright 2024 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.net;

/** The order of values in this enum should match {@code IpType} enum in Rust (libzonarosa-net). */
public enum IpType {
  UNKNOWN,
  IPv4,
  IPv6
}
