/*
 * Copyright 2013 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.controllers;

import static io.zonarosa.server.metrics.MetricsUtil.name;

import io.dropwizard.auth.Auth;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.zonarosa.server.auth.AuthenticatedDevice;
import io.zonarosa.server.metrics.UserAgentTagUtil;
import io.zonarosa.server.push.RedisMessageAvailabilityManager;
import io.zonarosa.websocket.session.WebSocketSession;
import io.zonarosa.websocket.session.WebSocketSessionContext;


@Path("/v1/keepalive")
@Tag(name = "Keep Alive")
public class KeepAliveController {

  private final Logger logger = LoggerFactory.getLogger(KeepAliveController.class);

  private final RedisMessageAvailabilityManager redisMessageAvailabilityManager;

  private static final String CLOSED_CONNECTION_AGE_DISTRIBUTION_NAME = name(KeepAliveController.class,
      "closedConnectionAge");


  public KeepAliveController(final RedisMessageAvailabilityManager redisMessageAvailabilityManager) {
    this.redisMessageAvailabilityManager = redisMessageAvailabilityManager;
  }

  @GET
  public Response getKeepAlive(@Auth Optional<AuthenticatedDevice> maybeAuth,
      @WebSocketSession WebSocketSessionContext context) {

    maybeAuth.ifPresent(auth -> {
      if (!redisMessageAvailabilityManager.isLocallyPresent(auth.accountIdentifier(), auth.deviceId())) {

        final Duration age = Duration.between(context.getClient().getCreated(), Instant.now());

        logger.debug("***** No local subscription found for {}::{}; age = {}ms, User-Agent = {}",
            auth.accountIdentifier(), auth.deviceId(), age.toMillis(),
            context.getClient().getUserAgent());

        context.getClient().close(1000, "OK");

        Timer.builder(CLOSED_CONNECTION_AGE_DISTRIBUTION_NAME)
            .tags(Tags.of(UserAgentTagUtil.getPlatformTag(context.getClient().getUserAgent())))
            .register(Metrics.globalRegistry)
            .record(age);
      }
    });

    return Response.ok().build();
  }

  @GET
  @Path("/provisioning")
  public Response getProvisioningKeepAlive() {
    return Response.ok().build();
  }

}
