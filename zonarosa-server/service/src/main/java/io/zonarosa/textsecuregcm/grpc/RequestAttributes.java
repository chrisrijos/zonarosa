/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.grpc;

import java.net.InetAddress;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;

public record RequestAttributes(InetAddress remoteAddress,
                                @Nullable String userAgent,
                                List<Locale.LanguageRange> acceptLanguage) {
}
