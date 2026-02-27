/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.server.subscriptions;

import java.time.Instant;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Payment details for a one-time payment specified by id
 *
 * @param id             The id of the payment in the payment processor
 * @param customMetadata Any custom metadata attached to the payment
 * @param status         The status of the payment in the payment processor
 * @param created        When the payment was created
 * @param chargeFailure  If present, additional information about why the payment failed. Will not be set if the status
 *                       is not {@link PaymentStatus#SUCCEEDED}
 */
public record PaymentDetails(String id,
                             Map<String, String> customMetadata,
                             PaymentStatus status,
                             Instant created,
                             @Nullable ChargeFailure chargeFailure) {}
