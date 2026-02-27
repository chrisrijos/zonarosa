/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.configuration;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.jackson.Discoverable;
import io.zonarosa.server.registration.RegistrationServiceClient;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = RegistrationServiceConfiguration.class)
public interface RegistrationServiceClientFactory extends Discoverable {

  RegistrationServiceClient build(Environment environment, Executor callbackExecutor,
      ScheduledExecutorService identityRefreshExecutor);
}
