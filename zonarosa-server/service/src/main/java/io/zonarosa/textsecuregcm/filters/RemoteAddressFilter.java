/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.filters;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.zonarosa.server.util.HttpServletRequestUtil;

/**
 * Sets a {@link HttpServletRequest} attribute (that will also be available as a
 * {@link jakarta.ws.rs.container.ContainerRequestContext} property) with the remote address of the connection, using
 * {@link HttpServletRequest#getRemoteAddr()}.
 */
public class RemoteAddressFilter implements Filter {

  public static final String REMOTE_ADDRESS_ATTRIBUTE_NAME = RemoteAddressFilter.class.getName() + ".remoteAddress";
  private static final Logger logger = LoggerFactory.getLogger(RemoteAddressFilter.class);


  public RemoteAddressFilter() {
  }

  @Override
  public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
      throws ServletException, IOException {

    if (request instanceof HttpServletRequest httpServletRequest) {

      final String remoteAddress = HttpServletRequestUtil.getRemoteAddress(httpServletRequest);
      request.setAttribute(REMOTE_ADDRESS_ATTRIBUTE_NAME, remoteAddress);

    } else {
      logger.warn("request was of unexpected type: {}", request.getClass());
    }

    chain.doFilter(request, response);
  }

}
