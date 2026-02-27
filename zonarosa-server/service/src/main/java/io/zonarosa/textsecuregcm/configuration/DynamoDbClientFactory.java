/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.configuration;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.dropwizard.jackson.Discoverable;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = DynamoDbClientConfiguration.class)
public interface DynamoDbClientFactory extends Discoverable {

  DynamoDbClient buildSyncClient(AwsCredentialsProvider awsCredentialsProvider, MetricPublisher metricPublisher);

  DynamoDbAsyncClient buildAsyncClient(AwsCredentialsProvider awsCredentialsProvider, MetricPublisher metricPublisher);
}
