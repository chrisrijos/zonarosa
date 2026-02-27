package io.zonarosa.server.entities;

import jakarta.validation.constraints.NotNull;

public record PhoneNumberDiscoverabilityRequest(@NotNull Boolean discoverableByPhoneNumber) {}
