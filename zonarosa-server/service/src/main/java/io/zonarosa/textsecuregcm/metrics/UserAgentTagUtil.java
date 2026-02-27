/*
 * Copyright 2013-2020 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.metrics;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.HttpHeaders;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import io.zonarosa.server.WhisperServerVersion;
import io.zonarosa.server.storage.ClientReleaseManager;
import io.zonarosa.server.util.ua.UnrecognizedUserAgentException;
import io.zonarosa.server.util.ua.UserAgent;
import io.zonarosa.server.util.ua.UserAgentUtil;

/**
 * Utility class for extracting platform/version metrics tags from User-Agent strings.
 */
public class UserAgentTagUtil {

  public static final String PLATFORM_TAG = "platform";
  public static final String VERSION_TAG = "clientVersion";
  public static final String OPERATING_SYSTEM_TAG = "operatingSystem";
  public static final String OPERATING_SYSTEM_VERSION_TAG = "operatingSystemVersion";
  public static final String LIBZONAROSA_VERSION_TAG = "libzonarosaVersion";

  public static final String SERVER_UA =
      String.format("ZonaRosa-Server/%s (%s)", WhisperServerVersion.getServerVersion(), UUID.randomUUID());

  private static final Pattern STANDARD_ADDITIONAL_SPECIFIERS_PATTERN =
      Pattern.compile("^(Android|iOS|Windows|macOS|Linux)[ /](\\S+) libzonarosa/([\\d.]+).*$", Pattern.CASE_INSENSITIVE);

  private UserAgentTagUtil() {
  }

  public static Tag getPlatformTag(final ContainerRequestContext containerRequestContext) {
    return getPlatformTag(containerRequestContext.getHeaderString(HttpHeaders.USER_AGENT));
  }

  public static Tag getPlatformTag(@Nullable final String userAgentString) {

    if (SERVER_UA.equals(userAgentString)) {
      return Tag.of(PLATFORM_TAG, "server");
    }

    UserAgent userAgent = null;

    try {
      userAgent = UserAgentUtil.parseUserAgentString(userAgentString);
    } catch (final UnrecognizedUserAgentException ignored) {
    }

    return getPlatformTag(userAgent);
  }

  public static Tag getPlatformTag(@Nullable final UserAgent userAgent) {
    return Tag.of(PLATFORM_TAG, userAgent != null ? userAgent.platform().name().toLowerCase() : "unrecognized");
  }

  public static Optional<Tag> getClientVersionTag(@Nullable final String userAgentString,
      final ClientReleaseManager clientReleaseManager) {

    try {
      return getClientVersionTag(UserAgentUtil.parseUserAgentString(userAgentString), clientReleaseManager);
    } catch (final UnrecognizedUserAgentException e) {
      return Optional.empty();
    }
  }

  public static Optional<Tag> getClientVersionTag(@Nullable final UserAgent userAgent,
      final ClientReleaseManager clientReleaseManager) {

    if (userAgent == null) {
      return Optional.empty();
    }

    return clientReleaseManager.isVersionActive(userAgent.platform(), userAgent.version())
        ? Optional.of(Tag.of(VERSION_TAG, userAgent.version().toString()))
        : Optional.empty();
  }

  public static Tags getAdditionalSpecifierTags(@Nullable final String userAgentString) {
    UserAgent userAgent = null;

    try {
      userAgent = UserAgentUtil.parseUserAgentString(userAgentString);
    } catch (final UnrecognizedUserAgentException ignored) {
    }

    return getAdditionalSpecifierTags(userAgent);
  }

  public static Tags getAdditionalSpecifierTags(@Nullable final UserAgent userAgent) {
    if (userAgent == null || StringUtils.isBlank(userAgent.additionalSpecifiers())) {
      return Tags.empty();
    }

    final Matcher matcher = STANDARD_ADDITIONAL_SPECIFIERS_PATTERN.matcher(userAgent.additionalSpecifiers());

    return matcher.matches()
        ? Tags.of(
        OPERATING_SYSTEM_TAG, matcher.group(1),
        OPERATING_SYSTEM_VERSION_TAG, matcher.group(2),
        LIBZONAROSA_VERSION_TAG, matcher.group(3))
        : Tags.empty();
  }
}
