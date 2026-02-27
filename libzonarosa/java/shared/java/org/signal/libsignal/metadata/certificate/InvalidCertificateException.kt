//
// Copyright 2023 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.metadata.certificate

public class InvalidCertificateException : Exception {
  public constructor(s: String) : super(s)

  public constructor(e: Exception) : super(e)
}
