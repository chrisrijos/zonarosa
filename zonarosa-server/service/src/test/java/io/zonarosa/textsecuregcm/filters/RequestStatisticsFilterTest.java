/*
 * Copyright 2021 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.filters;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.container.ContainerRequestContext;
import org.junit.jupiter.api.Test;
import io.zonarosa.server.metrics.TrafficSource;

class RequestStatisticsFilterTest {

  @Test
  void testFilter() throws Exception {

    final RequestStatisticsFilter requestStatisticsFilter = new RequestStatisticsFilter(TrafficSource.WEBSOCKET);

    final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

    when(requestContext.getLength()).thenReturn(-1);
    when(requestContext.getLength()).thenReturn(Integer.MAX_VALUE);
    when(requestContext.getLength()).thenThrow(RuntimeException.class);

    requestStatisticsFilter.filter(requestContext);
    requestStatisticsFilter.filter(requestContext);
    requestStatisticsFilter.filter(requestContext);
  }
}
