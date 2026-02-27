/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.grpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.zonarosa.chat.calling.quality.SimpleCallQualityGrpc;
import io.zonarosa.chat.calling.quality.SubmitCallQualitySurveyRequest;
import io.zonarosa.chat.calling.quality.SubmitCallQualitySurveyResponse;
import io.zonarosa.server.controllers.RateLimitExceededException;
import io.zonarosa.server.limits.RateLimiters;
import io.zonarosa.server.metrics.CallQualityInvalidArgumentsException;
import io.zonarosa.server.metrics.CallQualitySurveyManager;

public class CallQualitySurveyGrpcService extends SimpleCallQualityGrpc.CallQualityImplBase {

  private final CallQualitySurveyManager callQualitySurveyManager;
  private final RateLimiters rateLimiters;

  public CallQualitySurveyGrpcService(final CallQualitySurveyManager callQualitySurveyManager,
      final RateLimiters rateLimiters) {

    this.callQualitySurveyManager = callQualitySurveyManager;
    this.rateLimiters = rateLimiters;
  }

  @Override
  public SubmitCallQualitySurveyResponse submitCallQualitySurvey(final SubmitCallQualitySurveyRequest request)
      throws RateLimitExceededException {

    final String remoteAddress = RequestAttributesUtil.getRemoteAddress().getHostAddress();

    rateLimiters.getSubmitCallQualitySurveyLimiter().validate(remoteAddress);

    try {
      callQualitySurveyManager.submitCallQualitySurvey(request,
          remoteAddress,
          RequestAttributesUtil.getUserAgent().orElse(null));
    } catch (final CallQualityInvalidArgumentsException e) {
      throw e.getField()
          .map(fieldName -> GrpcExceptions.fieldViolation(fieldName, e.getMessage()))
          .orElseGet(() -> GrpcExceptions.invalidArguments(e.getMessage()));
    }

    return SubmitCallQualitySurveyResponse.getDefaultInstance();
  }
}
