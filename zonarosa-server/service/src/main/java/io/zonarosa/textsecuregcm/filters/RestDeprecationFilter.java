/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.filters;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.net.HttpHeaders;
import com.vdurmont.semver4j.Semver;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.zonarosa.server.auth.AuthenticatedDevice;
import io.zonarosa.server.configuration.dynamic.DynamicConfiguration;
import io.zonarosa.server.configuration.dynamic.DynamicRestDeprecationConfiguration.PlatformConfiguration;
import io.zonarosa.server.experiment.ExperimentEnrollmentManager;
import io.zonarosa.server.metrics.MetricsUtil;
import io.zonarosa.server.storage.DynamicConfigurationManager;
import io.zonarosa.server.util.ua.ClientPlatform;
import io.zonarosa.server.util.ua.UnrecognizedUserAgentException;
import io.zonarosa.server.util.ua.UserAgent;
import io.zonarosa.server.util.ua.UserAgentUtil;

public class RestDeprecationFilter implements ContainerRequestFilter {

  private static final String AUTHENTICATED_EXPERIMENT_NAME = "restDeprecation";
  private static final String DEPRECATED_REST_COUNTER_NAME = MetricsUtil.name(RestDeprecationFilter.class, "blockedRestRequest");

  private static final Logger log = LoggerFactory.getLogger(RestDeprecationFilter.class);

  final DynamicConfigurationManager<DynamicConfiguration> dynamicConfigurationManager;
  final ExperimentEnrollmentManager experimentEnrollmentManager;
  final Supplier<Random> random;

  public RestDeprecationFilter(
      final DynamicConfigurationManager<DynamicConfiguration> dynamicConfigurationManager,
      final ExperimentEnrollmentManager experimentEnrollmentManager) {
    this(dynamicConfigurationManager, experimentEnrollmentManager, ThreadLocalRandom::current);
  }

  @VisibleForTesting
  public RestDeprecationFilter(
      final DynamicConfigurationManager<DynamicConfiguration> dynamicConfigurationManager,
      final ExperimentEnrollmentManager experimentEnrollmentManager,
      final Supplier<Random> random) {
    this.dynamicConfigurationManager = dynamicConfigurationManager;
    this.experimentEnrollmentManager = experimentEnrollmentManager;
    this.random = random;
  }

  @Override
  public void filter(final ContainerRequestContext requestContext) throws IOException {

    final String userAgentString = requestContext.getHeaderString(HttpHeaders.USER_AGENT);

    try {
      final UserAgent userAgent = UserAgentUtil.parseUserAgentString(userAgentString);
      final ClientPlatform platform = userAgent.platform();
      final Semver version = userAgent.version();
      final PlatformConfiguration config = dynamicConfigurationManager.getConfiguration().restDeprecation().platforms().get(platform);
      if (config == null) {
        return;
      }
      if (!isEnrolled(requestContext, config.universalRolloutPercent())) {
        return;
      }
      if (version.isGreaterThanOrEqualTo(config.minimumRestFreeVersion())) {
        Metrics.counter(
            DEPRECATED_REST_COUNTER_NAME, Tags.of("platform", platform.name().toLowerCase(), "version", version.toString()))
            .increment();
        throw new WebApplicationException("use websockets", 498);
      }
    } catch (final UnrecognizedUserAgentException e) {
      return;                   // at present we're only interested in experimenting on known clients
    }
  }

  private boolean isEnrolled(final ContainerRequestContext requestContext, int universalRolloutPercent) {
    if (random.get().nextInt(100) < universalRolloutPercent) {
      return true;
    }

    final SecurityContext securityContext = requestContext.getSecurityContext();

    if (securityContext == null || securityContext.getUserPrincipal() == null) {
      return false;
    }

    if (securityContext.getUserPrincipal() instanceof AuthenticatedDevice authenticatedDevice) {
      return experimentEnrollmentManager.isEnrolled(authenticatedDevice.accountIdentifier(), AUTHENTICATED_EXPERIMENT_NAME);
    } else {
      log.error("Security context was not null but user principal was of type {}", securityContext.getUserPrincipal().getClass().getName());
      return false;
    }
  }
}
