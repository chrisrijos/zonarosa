/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.server.subscriptions;

import java.time.Instant;
import javax.annotation.Nullable;

public record SubscriptionInformation(
    SubscriptionPrice price,
    long level,
    Instant billingCycleAnchor,
    Instant endOfCurrentPeriod,
    boolean active,
    boolean cancelAtPeriodEnd,
    SubscriptionStatus status,
    PaymentProvider paymentProvider,
    PaymentMethod paymentMethod,
    boolean paymentProcessing,
    @Nullable ChargeFailure chargeFailure) {}
