/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.configuration;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.constraints.NotNull;
import io.zonarosa.server.configuration.secrets.SecretString;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = StaticAwsCredentialsFactory.class)
@JsonTypeName("static")
public record StaticAwsCredentialsFactory(@NotNull SecretString accessKeyId,
                                          @NotNull SecretString secretAccessKey) implements
    AwsCredentialsProviderFactory {

  @Override
  public AwsCredentialsProvider build() {
    return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId.value(), secretAccessKey.value()));
  }
}
