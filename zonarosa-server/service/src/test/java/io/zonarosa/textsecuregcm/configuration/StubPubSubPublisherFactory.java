/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.configuration;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.api.core.ApiFutures;
import com.google.cloud.pubsub.v1.PublisherInterface;
import java.util.UUID;

@JsonTypeName("stub")
public class StubPubSubPublisherFactory implements PubSubPublisherFactory {

  @Override
  public PublisherInterface build() {
    return message -> ApiFutures.immediateFuture(UUID.randomUUID().toString());
  }
}
