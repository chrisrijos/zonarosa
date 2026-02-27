/*
 * Copyright 2013 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.controllers;

import io.dropwizard.auth.Auth;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import io.zonarosa.server.auth.AuthenticatedDevice;
import io.zonarosa.server.auth.ExternalServiceCredentials;
import io.zonarosa.server.auth.ExternalServiceCredentialsGenerator;
import io.zonarosa.server.configuration.PaymentsServiceConfiguration;
import io.zonarosa.server.currency.CurrencyConversionManager;
import io.zonarosa.server.entities.CurrencyConversionEntityList;

@Path("/v1/payments")
@Tag(name = "Payments")
public class PaymentsController {

  private final ExternalServiceCredentialsGenerator paymentsServiceCredentialsGenerator;
  private final CurrencyConversionManager currencyManager;


  public static ExternalServiceCredentialsGenerator credentialsGenerator(final PaymentsServiceConfiguration cfg) {
    return ExternalServiceCredentialsGenerator
        .builder(cfg.userAuthenticationTokenSharedSecret())
        .prependUsername(true)
        .build();
  }

  public PaymentsController(final CurrencyConversionManager currencyManager,
      final ExternalServiceCredentialsGenerator paymentsServiceCredentialsGenerator) {
    this.currencyManager = currencyManager;
    this.paymentsServiceCredentialsGenerator = paymentsServiceCredentialsGenerator;
  }

  @GET
  @Path("/auth")
  @Produces(MediaType.APPLICATION_JSON)
  public ExternalServiceCredentials getAuth(final @Auth AuthenticatedDevice auth) {
    return paymentsServiceCredentialsGenerator.generateForUuid(auth.accountIdentifier());
  }

  @GET
  @Path("/conversions")
  @Produces(MediaType.APPLICATION_JSON)
  public CurrencyConversionEntityList getConversions(final @Auth AuthenticatedDevice auth) {
    return currencyManager.getCurrencyConversions().orElseThrow();
  }
}
