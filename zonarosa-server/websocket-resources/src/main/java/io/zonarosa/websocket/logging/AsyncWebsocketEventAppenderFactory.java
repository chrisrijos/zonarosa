/*
 * Copyright 2013-2020 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.websocket.logging;

import ch.qos.logback.core.AsyncAppenderBase;
import io.dropwizard.logging.common.async.AsyncAppenderFactory;

public class AsyncWebsocketEventAppenderFactory implements AsyncAppenderFactory<WebsocketEvent> {
  @Override
  public AsyncAppenderBase<WebsocketEvent> build() {
    return new AsyncAppenderBase<WebsocketEvent>() {
      @Override
      protected void preprocess(WebsocketEvent event) {
        event.prepareForDeferredProcessing();
      }
    };
  }
}
