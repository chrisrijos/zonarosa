package io.zonarosa.server.controllers;

import io.dropwizard.auth.Auth;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import io.zonarosa.libzonarosa.protocol.ServiceId;
import io.zonarosa.libzonarosa.zkgroup.GenericServerSecretParams;
import io.zonarosa.libzonarosa.zkgroup.InvalidInputException;
import io.zonarosa.libzonarosa.zkgroup.calllinks.CreateCallLinkCredentialRequest;
import io.zonarosa.server.auth.AuthenticatedDevice;
import io.zonarosa.server.entities.CreateCallLinkCredential;
import io.zonarosa.server.entities.GetCreateCallLinkCredentialsRequest;
import io.zonarosa.server.limits.RateLimiters;

@Path("/v1/call-link")
@io.swagger.v3.oas.annotations.tags.Tag(name = "CallLink")
public class CallLinkController {
  private final RateLimiters rateLimiters;
  private final GenericServerSecretParams genericServerSecretParams;

  public CallLinkController(
      final RateLimiters rateLimiters,
      final GenericServerSecretParams genericServerSecretParams
  ) {
    this.rateLimiters = rateLimiters;
    this.genericServerSecretParams = genericServerSecretParams;
  }

  @POST
  @Path("/create-auth")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Generate a credential for creating call links",
      description = """
          Generate a credential over a truncated timestamp, room ID, and account UUID. With zero knowledge
          group infrastructure, the server does not know the room ID.
          """
  )
  @ApiResponse(responseCode = "200", description = "`JSON` with generated credentials.", useReturnTypeSchema = true)
  @ApiResponse(responseCode = "400", description = "Invalid create call link credential request.")
  @ApiResponse(responseCode = "401", description = "Account authentication check failed.")
  @ApiResponse(responseCode = "422", description = "Invalid request format.")
  @ApiResponse(responseCode = "429", description = "Ratelimited.")
  public CreateCallLinkCredential getCreateAuth(
      final @Auth AuthenticatedDevice auth,
      final @NotNull @Valid GetCreateCallLinkCredentialsRequest request
  ) throws RateLimitExceededException {

    rateLimiters.getCreateCallLinkLimiter().validate(auth.accountIdentifier());

    final Instant truncatedDayTimestamp = Instant.now().truncatedTo(ChronoUnit.DAYS);

    CreateCallLinkCredentialRequest createCallLinkCredentialRequest;
    try {
      createCallLinkCredentialRequest = new CreateCallLinkCredentialRequest(request.createCallLinkCredentialRequest());
    } catch (InvalidInputException e) {
      throw new BadRequestException("Invalid create call link credential request", e);
    }

    return new CreateCallLinkCredential(
        createCallLinkCredentialRequest.issueCredential(new ServiceId.Aci(auth.accountIdentifier()), truncatedDayTimestamp, genericServerSecretParams).serialize(),
        truncatedDayTimestamp.getEpochSecond()
    );
  }
}
