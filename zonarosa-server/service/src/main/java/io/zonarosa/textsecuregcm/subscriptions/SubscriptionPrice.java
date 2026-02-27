/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.server.subscriptions;

import java.math.BigDecimal;

public record SubscriptionPrice(String currency, BigDecimal amount) {}
