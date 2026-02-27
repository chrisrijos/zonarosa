/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.storage;

import io.zonarosa.server.util.SystemMapper;
import java.io.IOException;

public class AccountUtil {

  static Account cloneAccountAsNotStale(final Account account) {
    try {
      return SystemMapper.jsonMapper().readValue(
          SystemMapper.jsonMapper().writeValueAsBytes(account), Account.class);
    } catch (final IOException e) {
      // this should really, truly, never happen
      throw new IllegalArgumentException(e);
    }
  }
}
