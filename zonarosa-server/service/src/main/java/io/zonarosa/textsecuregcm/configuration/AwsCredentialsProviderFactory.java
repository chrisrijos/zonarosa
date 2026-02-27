/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.configuration;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.dropwizard.jackson.Discoverable;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = DefaultAwsCredentialsFactory.class)
public interface AwsCredentialsProviderFactory extends Discoverable {

  AwsCredentialsProvider build();
}
