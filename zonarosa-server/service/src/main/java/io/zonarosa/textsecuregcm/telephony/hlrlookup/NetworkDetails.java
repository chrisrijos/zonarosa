/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.telephony.hlrlookup;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
record NetworkDetails(String name,
                      String mccmnc,
                      String countryName,
                      String countryIso3,
                      String area,
                      String countryPrefix) {
}
