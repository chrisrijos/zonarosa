/*
 * Copyright 2021 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.entities;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.zonarosa.server.identity.ServiceIdentifier;
import io.zonarosa.server.util.ServiceIdentifierAdapter;

public record AccountStaleDevices(@JsonSerialize(using = ServiceIdentifierAdapter.ServiceIdentifierSerializer.class)
                                  @JsonDeserialize(using = ServiceIdentifierAdapter.ServiceIdentifierDeserializer.class)
                                  ServiceIdentifier uuid,

                                  StaleDevicesResponse devices) {
}
