package io.zonarosa.server.entities;

import jakarta.validation.constraints.NotEmpty;


public record GetCreateCallLinkCredentialsRequest(@NotEmpty byte[] createCallLinkCredentialRequest) {}
