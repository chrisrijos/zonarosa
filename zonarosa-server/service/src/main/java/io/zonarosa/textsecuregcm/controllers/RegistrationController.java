/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.controllers;

import static io.zonarosa.server.metrics.MetricsUtil.name;

import com.google.common.net.HttpHeaders;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import io.zonarosa.server.auth.BasicAuthorizationHeader;
import io.zonarosa.server.auth.PhoneVerificationTokenManager;
import io.zonarosa.server.auth.RegistrationLockVerificationManager;
import io.zonarosa.server.entities.AccountCreationResponse;
import io.zonarosa.server.entities.AccountIdentityResponse;
import io.zonarosa.server.entities.PhoneVerificationRequest;
import io.zonarosa.server.entities.RegistrationLockFailure;
import io.zonarosa.server.entities.RegistrationRequest;
import io.zonarosa.server.limits.RateLimiters;
import io.zonarosa.server.metrics.UserAgentTagUtil;
import io.zonarosa.server.storage.Account;
import io.zonarosa.server.storage.AccountsManager;
import io.zonarosa.server.storage.DeviceCapability;
import io.zonarosa.server.storage.DeviceSpec;
import io.zonarosa.server.util.HeaderUtils;
import io.zonarosa.server.util.Util;

@Path("/v1/registration")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Registration")
public class RegistrationController {

  private static final DistributionSummary REREGISTRATION_IDLE_DAYS_DISTRIBUTION = DistributionSummary
      .builder(name(RegistrationController.class, "reregistrationIdleDays"))
      .distributionStatisticExpiry(Duration.ofHours(2))
      .register(Metrics.globalRegistry);

  private static final String ACCOUNT_CREATED_COUNTER_NAME = name(RegistrationController.class, "accountCreated");
  private static final String COUNTRY_CODE_TAG_NAME = "countryCode";
  private static final String REGION_CODE_TAG_NAME = "regionCode";
  private static final String VERIFICATION_TYPE_TAG_NAME = "verification";

  private final AccountsManager accounts;
  private final PhoneVerificationTokenManager phoneVerificationTokenManager;
  private final RegistrationLockVerificationManager registrationLockVerificationManager;
  private final RateLimiters rateLimiters;

  public RegistrationController(final AccountsManager accounts,
                                final PhoneVerificationTokenManager phoneVerificationTokenManager,
                                final RegistrationLockVerificationManager registrationLockVerificationManager,
                                final RateLimiters rateLimiters) {

    this.accounts = accounts;
    this.phoneVerificationTokenManager = phoneVerificationTokenManager;
    this.registrationLockVerificationManager = registrationLockVerificationManager;
    this.rateLimiters = rateLimiters;
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Registers an account",
  description = """
      Registers a new account or attempts to “re-register” an existing account. It is expected that a well-behaved client
      could make up to three consecutive calls to this API:
      1. gets 423 from existing registration lock \n
      2. gets 409 from device available for transfer \n
      3. success \n
      """)
  @ApiResponse(responseCode = "200", description = "Account creation succeeded", useReturnTypeSchema = true)
  @ApiResponse(responseCode = "401", description = "The session identified in the request is not verified")
  @ApiResponse(responseCode = "403", description = "Verification failed for the provided Registration Recovery Password")
  @ApiResponse(responseCode = "409", description = "The caller has not explicitly elected to skip transferring data from another device, but a device transfer is technically possible")
  @ApiResponse(responseCode = "422", description = "The request did not pass validation")
  @ApiResponse(responseCode = "423", content = @Content(schema = @Schema(implementation = RegistrationLockFailure.class)))
  @ApiResponse(responseCode = "429", description = "Too many attempts", headers = @Header(
      name = "Retry-After",
      description = "If present, an positive integer indicating the number of seconds before a subsequent attempt could succeed"))
  public AccountCreationResponse register(
      @HeaderParam(HttpHeaders.AUTHORIZATION) @NotNull final BasicAuthorizationHeader authorizationHeader,
      @HeaderParam(HeaderUtils.X_ZONAROSA_AGENT) final String zonarosaAgent,
      @HeaderParam(HttpHeaders.USER_AGENT) final String userAgent,
      @NotNull @Valid final RegistrationRequest registrationRequest,
      @Context final ContainerRequestContext requestContext) throws RateLimitExceededException, InterruptedException {

    final String number = authorizationHeader.getUsername();
    final String password = authorizationHeader.getPassword();

    if (!registrationRequest.isEverySignedKeyValid(userAgent)) {
      throw new WebApplicationException("Invalid signature", 422);
    }

    rateLimiters.getRegistrationLimiter().validate(number);

    final PhoneVerificationRequest.VerificationType verificationType = phoneVerificationTokenManager.verify(
        requestContext, number, registrationRequest);

    final Optional<Account> existingAccount = accounts.getByE164(number);

    existingAccount.ifPresent(account -> {
      final Instant accountLastSeen = Instant.ofEpochMilli(account.getLastSeen());
      final Duration timeSinceLastSeen = Duration.between(accountLastSeen, Instant.now());
      REREGISTRATION_IDLE_DAYS_DISTRIBUTION.record(timeSinceLastSeen.toDays());
    });

    if (!registrationRequest.skipDeviceTransfer() && existingAccount.map(account -> account.hasCapability(DeviceCapability.TRANSFER)).orElse(false)) {
      // If a device transfer is possible, clients must explicitly opt out of a transfer (i.e. after prompting the user)
      // before we'll let them create a new account "from scratch"
      throw new WebApplicationException(Response.status(409, "device transfer available").build());
    }

    if (existingAccount.isPresent()) {
      registrationLockVerificationManager.verifyRegistrationLock(existingAccount.get(),
          registrationRequest.accountAttributes().getRegistrationLock(),
          userAgent, RegistrationLockVerificationManager.Flow.REGISTRATION, verificationType);
    }

    final Account account = accounts.create(number,
        registrationRequest.accountAttributes(),
        existingAccount.map(Account::getBadges).orElseGet(ArrayList::new),
        registrationRequest.aciIdentityKey(),
        registrationRequest.pniIdentityKey(),
        new DeviceSpec(
            registrationRequest.accountAttributes().getName(),
            password,
            zonarosaAgent,
            registrationRequest.accountAttributes().getCapabilities(),
            registrationRequest.accountAttributes().getRegistrationId(),
            registrationRequest.accountAttributes().getPhoneNumberIdentityRegistrationId(),
            registrationRequest.accountAttributes().getFetchesMessages(),
            registrationRequest.deviceActivationRequest().apnToken(),
            registrationRequest.deviceActivationRequest().gcmToken(),
            registrationRequest.deviceActivationRequest().aciSignedPreKey(),
            registrationRequest.deviceActivationRequest().pniSignedPreKey(),
            registrationRequest.deviceActivationRequest().aciPqLastResortPreKey(),
            registrationRequest.deviceActivationRequest().pniPqLastResortPreKey()),
        userAgent);

    Metrics.counter(ACCOUNT_CREATED_COUNTER_NAME, Tags.of(UserAgentTagUtil.getPlatformTag(userAgent),
            Tag.of(COUNTRY_CODE_TAG_NAME, Util.getCountryCode(number)),
            Tag.of(REGION_CODE_TAG_NAME, Util.getRegion(number)),
            Tag.of(VERIFICATION_TYPE_TAG_NAME, verificationType.name())))
        .increment();

    final AccountIdentityResponse identityResponse = new AccountIdentityResponseBuilder(account)
        // If there was an existing account, return whether it could have had something in the storage service
        .storageCapable(existingAccount
            .map(a -> a.hasCapability(DeviceCapability.STORAGE))
            .orElse(false))
        .build();

    return new AccountCreationResponse(identityResponse, existingAccount.isPresent());
  }

}
