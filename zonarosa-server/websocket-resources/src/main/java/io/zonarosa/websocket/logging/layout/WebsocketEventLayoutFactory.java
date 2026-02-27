/*
 * Copyright 2013-2020 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.websocket.logging.layout;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.pattern.PatternLayoutBase;
import io.dropwizard.logging.common.layout.LayoutFactory;
import io.zonarosa.websocket.logging.WebsocketEvent;

import java.util.TimeZone;

public class WebsocketEventLayoutFactory implements LayoutFactory<WebsocketEvent> {
  @Override
  public PatternLayoutBase<WebsocketEvent> build(LoggerContext context, TimeZone timeZone) {
    return new WebsocketEventLayout(context);
  }
}
