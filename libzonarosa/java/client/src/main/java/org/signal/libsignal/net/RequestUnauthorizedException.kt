//
// Copyright 2025 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.net

import java.io.IOException

/**
 * A request requires authorization, but the provided authorization (if any) was incorrect or
 * insufficient.
 *
 * See the specific request docs for more information.
 */
public class RequestUnauthorizedException :
  IOException,
  MultiRecipientSendFailure {
  public constructor(message: String) : super(message) {}
}
