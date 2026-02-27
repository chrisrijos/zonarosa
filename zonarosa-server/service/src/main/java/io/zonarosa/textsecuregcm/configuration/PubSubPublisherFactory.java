/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.configuration;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.cloud.pubsub.v1.PublisherInterface;
import io.dropwizard.jackson.Discoverable;
import java.io.IOException;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = DefaultPubSubPublisherFactory.class)
public interface PubSubPublisherFactory extends Discoverable {

  PublisherInterface build() throws IOException;
}
