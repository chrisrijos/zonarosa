/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.configuration.dynamic;

/**
 * @param enableLettuceRemoteTag whether the `remote` tag should be added. Note: although this is dynamic, meters are
 *                               cached after creation, so changes will only affect servers launched after the change.
 * @param enableAwsSdkMetrics whether to record AWS SDK metrics. Note: although this is dynamic, meters are cached after
 *                            creation, so changes will only affect servers launched after the change.
 */
public record DynamicMetricsConfiguration(boolean enableLettuceRemoteTag, boolean enableAwsSdkMetrics) {
}
