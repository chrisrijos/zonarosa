/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import io.zonarosa.chat.calling.quality.SubmitCallQualitySurveyRequest;
import io.zonarosa.server.auth.AuthenticatedDevice;
import io.zonarosa.server.mappers.RateLimitExceededExceptionMapper;
import io.zonarosa.server.metrics.CallQualityInvalidArgumentsException;
import io.zonarosa.server.metrics.CallQualitySurveyManager;
import io.zonarosa.server.tests.util.AuthHelper;
import io.zonarosa.server.util.SystemMapper;
import io.zonarosa.server.util.TestRemoteAddressFilterProvider;
import java.util.List;

@ExtendWith(DropwizardExtensionsSupport.class)
class CallQualitySurveyControllerTest {

  private static final CallQualitySurveyManager CALL_QUALITY_SURVEY_MANAGER = mock(CallQualitySurveyManager.class);

  private static final String USER_AGENT = "ZonaRosa-iOS/7.78.0.1041 iOS/18.3.2 libzonarosa/0.80.3";
  private static final String REMOTE_ADDRESS = "127.0.0.1";

  private static final ResourceExtension RESOURCE_EXTENSION = ResourceExtension.builder()
      .addProvider(AuthHelper.getAuthFilter())
      .addProvider(new AuthValueFactoryProvider.Binder<>(AuthenticatedDevice.class))
      .addProvider(new RateLimitExceededExceptionMapper())
      .addProvider(new TestRemoteAddressFilterProvider(REMOTE_ADDRESS))
      .setMapper(SystemMapper.jsonMapper())
      .setTestContainerFactory(new GrizzlyWebTestContainerFactory())
      .addResource(new CallQualitySurveyController(CALL_QUALITY_SURVEY_MANAGER))
      .build();

  @BeforeEach
  void setUp() {
    reset(CALL_QUALITY_SURVEY_MANAGER);
  }

  @Test
  void submitCallQualitySurvey() throws CallQualityInvalidArgumentsException {
    final SubmitCallQualitySurveyRequest request = SubmitCallQualitySurveyRequest.getDefaultInstance();

    try (final Response response = RESOURCE_EXTENSION.getJerseyTest()
        .target("/v1/call_quality_survey")
        .request()
        .header("User-Agent", USER_AGENT)
        .put(Entity.entity(request.toByteArray(), MediaType.APPLICATION_OCTET_STREAM_TYPE))) {

      assertEquals(204, response.getStatus());
      verify(CALL_QUALITY_SURVEY_MANAGER).submitCallQualitySurvey(request, REMOTE_ADDRESS, USER_AGENT);
    }
  }

  @Test
  void submitCallQualitySurveyAuthenticated() throws CallQualityInvalidArgumentsException {
    final SubmitCallQualitySurveyRequest request = SubmitCallQualitySurveyRequest.getDefaultInstance();

    try (final Response response = RESOURCE_EXTENSION.getJerseyTest()
        .target("/v1/call_quality_survey")
        .request()
        .header("User-Agent", USER_AGENT)
        .header("Authorization", AuthHelper.getAuthHeader(AuthHelper.VALID_UUID, AuthHelper.VALID_PASSWORD))
        .put(Entity.entity(request.toByteArray(), MediaType.APPLICATION_OCTET_STREAM_TYPE))) {

      assertEquals(403, response.getStatus());
      verify(CALL_QUALITY_SURVEY_MANAGER, never()).submitCallQualitySurvey(any(), any(), any());
    }
  }

  @Test
  void submitCallQualitySurveyInvalidArgument() throws CallQualityInvalidArgumentsException {
    final SubmitCallQualitySurveyRequest request = SubmitCallQualitySurveyRequest.getDefaultInstance();

    doThrow(new CallQualityInvalidArgumentsException("test"))
        .when(CALL_QUALITY_SURVEY_MANAGER).submitCallQualitySurvey(request, REMOTE_ADDRESS, USER_AGENT);

    try (final Response response = RESOURCE_EXTENSION.getJerseyTest()
        .target("/v1/call_quality_survey")
        .request()
        .header("User-Agent", USER_AGENT)
        .put(Entity.entity(request.toByteArray(), MediaType.APPLICATION_OCTET_STREAM_TYPE))) {

      assertEquals(422, response.getStatus());
    }
  }
}
