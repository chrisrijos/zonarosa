/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.configuration;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.dropwizard.jackson.Discoverable;
import io.zonarosa.server.s3.S3ObjectMonitor;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import java.util.concurrent.ScheduledExecutorService;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = MonitoredS3ObjectConfiguration.class)
public interface S3ObjectMonitorFactory extends Discoverable {

  S3ObjectMonitor build(AwsCredentialsProvider awsCredentialsProvider,
      ScheduledExecutorService refreshExecutorService);
}
