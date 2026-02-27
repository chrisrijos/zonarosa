/*
 * Copyright 2013-2021 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.util.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import org.glassfish.jersey.server.ExtendedUriInfo;
import org.glassfish.jersey.uri.UriTemplate;
import org.junit.jupiter.api.Test;

class UriInfoUtilTest {

  @Test
  void testGetPathTemplate() {
    final UriTemplate firstComponent = new UriTemplate("/first");
    final UriTemplate secondComponent = new UriTemplate("/second");
    final UriTemplate thirdComponent = new UriTemplate("/{param}/{moreDifferentParam}");

    final ExtendedUriInfo uriInfo = mock(ExtendedUriInfo.class);
    when(uriInfo.getMatchedTemplates()).thenReturn(Arrays.asList(thirdComponent, secondComponent, firstComponent));

    assertEquals("/first/second/{param}/{moreDifferentParam}", UriInfoUtil.getPathTemplate(uriInfo));
  }

}
