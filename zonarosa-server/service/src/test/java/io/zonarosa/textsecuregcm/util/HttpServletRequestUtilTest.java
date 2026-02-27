/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class HttpServletRequestUtilTest {

  @ParameterizedTest
  @CsvSource({
      "127.0.0.1, 127.0.0.1",
      "[0:0:0:0:0:0:0:1], 0:0:0:0:0:0:0:1"
  })
  void testGetRemoteAddress(final String remoteAddr, final String expectedRemoteAddr) {
    final HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
    when(httpServletRequest.getRemoteAddr()).thenReturn(remoteAddr);

    assertEquals(expectedRemoteAddr, HttpServletRequestUtil.getRemoteAddress(httpServletRequest));
  }
}
