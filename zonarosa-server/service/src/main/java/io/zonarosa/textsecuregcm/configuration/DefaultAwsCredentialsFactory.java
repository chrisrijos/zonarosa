/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.configuration;

import com.fasterxml.jackson.annotation.JsonTypeName;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.WebIdentityTokenFileCredentialsProvider;

@JsonTypeName("default")
public record DefaultAwsCredentialsFactory() implements AwsCredentialsProviderFactory {

  public AwsCredentialsProvider build() {
    return WebIdentityTokenFileCredentialsProvider.create();
  }
}
