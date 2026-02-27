/*
 * Copyright 2021 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Set;

import org.assertj.core.api.AbstractStringAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import io.zonarosa.server.configuration.dynamic.DynamicConfiguration;
import io.zonarosa.server.configuration.dynamic.DynamicMetricsConfiguration;
import io.zonarosa.server.storage.DynamicConfigurationManager;


class MetricsUtilTest {

  @Test
  void name() {

    assertEquals("chat.MetricsUtilTest.metric", MetricsUtil.name(MetricsUtilTest.class, "metric"));
    assertEquals("chat.MetricsUtilTest.namespace.metric",
        MetricsUtil.name(MetricsUtilTest.class, "namespace", "metric"));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void lettuceTagRejection(final boolean enableLettuceRemoteTag) {
    final DynamicConfiguration dynamicConfiguration = mock(DynamicConfiguration.class);
    final DynamicMetricsConfiguration metricsConfiguration = new DynamicMetricsConfiguration(enableLettuceRemoteTag, false);
    when(dynamicConfiguration.getMetricsConfiguration()).thenReturn(metricsConfiguration);
    @SuppressWarnings("unchecked") final DynamicConfigurationManager<DynamicConfiguration> dynamicConfigurationManager =
        mock(DynamicConfigurationManager.class);
    when(dynamicConfigurationManager.getConfiguration()).thenReturn(dynamicConfiguration);

    final MeterRegistry registry = new SimpleMeterRegistry();
    MetricsUtil.configureMeterFilters(registry.config(), dynamicConfigurationManager);

    registry.counter("lettuce.command.completion.max", "command", "hello", "remote", "world", "allowed", "!").increment();
    final List<Meter> meters = registry.getMeters();
    assertThat(meters).hasSize(1);

    final Meter meter = meters.getFirst();
    assertThat(meter.getId().getName()).isEqualTo("chat.lettuce.command.completion.max");
    assertThat(meter.getId().getTag("command")).isNull();
    AbstractStringAssert<?> remoteTag = assertThat(meter.getId().getTag("remote"));

    if (enableLettuceRemoteTag) {
      remoteTag.isNotNull();
    } else {
      remoteTag.isNull();
    }
    assertThat(meter.getId().getTag("allowed")).isNotNull();
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void awsSdkMetricRejection(final boolean enableAwsSdkMetrics) {
    @SuppressWarnings("unchecked") final DynamicConfigurationManager<DynamicConfiguration> dynamicConfigurationManager =
        mock(DynamicConfigurationManager.class);

    final DynamicConfiguration dynamicConfiguration = mock(DynamicConfiguration.class);
    final DynamicMetricsConfiguration metricsConfiguration = new DynamicMetricsConfiguration(false, enableAwsSdkMetrics);

    when(dynamicConfigurationManager.getConfiguration()).thenReturn(dynamicConfiguration);
    when(dynamicConfiguration.getMetricsConfiguration()).thenReturn(metricsConfiguration);

    final MeterRegistry registry = new SimpleMeterRegistry();
    MetricsUtil.configureMeterFilters(registry.config(), dynamicConfigurationManager);
    registry.counter("chat.MicrometerAwsSdkMetricPublisher.days_since_last_incident").increment();

    assertThat(registry.getMeters()).hasSize(enableAwsSdkMetrics ? 1 : 0);
  }

}
