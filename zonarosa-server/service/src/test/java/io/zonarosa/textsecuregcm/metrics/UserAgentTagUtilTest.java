/*
 * Copyright 2013-2022 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import io.zonarosa.server.storage.ClientReleaseManager;

class UserAgentTagUtilTest {

  @ParameterizedTest
  @MethodSource
  void getPlatformTag(final String userAgent, final Tag expectedTag) {
    assertEquals(expectedTag, UserAgentTagUtil.getPlatformTag(userAgent));
  }

  private static Stream<Arguments> getPlatformTag() {
    return Stream.of(
        Arguments.of("This is obviously not a reasonable User-Agent string.", Tag.of(UserAgentTagUtil.PLATFORM_TAG, "unrecognized")),
        Arguments.of(null, Tag.of(UserAgentTagUtil.PLATFORM_TAG, "unrecognized")),
        Arguments.of("ZonaRosa-Android/4.53.7 (Android 8.1)", Tag.of(UserAgentTagUtil.PLATFORM_TAG, "android")),
        Arguments.of("ZonaRosa-Desktop/1.2.3", Tag.of(UserAgentTagUtil.PLATFORM_TAG, "desktop")),
        Arguments.of("ZonaRosa-iOS/3.9.0 (iPhone; iOS 12.2; Scale/3.00)", Tag.of(UserAgentTagUtil.PLATFORM_TAG, "ios")),
        Arguments.of("ZonaRosa-Android/1.2.3 (Android 8.1)", Tag.of(UserAgentTagUtil.PLATFORM_TAG, "android")),
        Arguments.of("ZonaRosa-Desktop/3.9.0", Tag.of(UserAgentTagUtil.PLATFORM_TAG, "desktop")),
        Arguments.of("ZonaRosa-iOS/4.53.7 (iPhone; iOS 12.2; Scale/3.00)", Tag.of(UserAgentTagUtil.PLATFORM_TAG, "ios")),
        Arguments.of("ZonaRosa-Android/4.68.3 (Android 9)", Tag.of(UserAgentTagUtil.PLATFORM_TAG, "android")),
        Arguments.of("ZonaRosa-Android/1.2.3 (Android 4.3)", Tag.of(UserAgentTagUtil.PLATFORM_TAG, "android")),
        Arguments.of("ZonaRosa-Android/4.68.3.0-bobsbootlegclient", Tag.of(UserAgentTagUtil.PLATFORM_TAG, "android")),
        Arguments.of("ZonaRosa-Desktop/1.22.45-foo-0", Tag.of(UserAgentTagUtil.PLATFORM_TAG, "desktop")),
        Arguments.of("ZonaRosa-Desktop/1.34.5-beta.1-fakeclientemporium", Tag.of(UserAgentTagUtil.PLATFORM_TAG, "desktop")),
        Arguments.of("ZonaRosa-Desktop/1.32.0-beta.3", Tag.of(UserAgentTagUtil.PLATFORM_TAG, "desktop")),
        Arguments.of(UserAgentTagUtil.SERVER_UA, Tag.of(UserAgentTagUtil.PLATFORM_TAG, "server")),
        Arguments.of("ZonaRosa-Server/1.2.3 (" + UUID.randomUUID() + ")", Tag.of(UserAgentTagUtil.PLATFORM_TAG, "unrecognized"))
    );
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  @ParameterizedTest
  @MethodSource
  void getClientVersionTag(final String userAgent, final boolean isVersionLive, final Optional<Tag> expectedTag) {
    final ClientReleaseManager clientReleaseManager = mock(ClientReleaseManager.class);
    when(clientReleaseManager.isVersionActive(any(), any())).thenReturn(isVersionLive);

    assertEquals(expectedTag, UserAgentTagUtil.getClientVersionTag(userAgent, clientReleaseManager));
  }

  private static Stream<Arguments> getClientVersionTag() {
    return Stream.of(
        Arguments.of("ZonaRosa-Android/1.2.3 (Android 9)",
            true,
            Optional.of(Tag.of(UserAgentTagUtil.VERSION_TAG, "1.2.3"))),

        Arguments.of("ZonaRosa-Android/1.2.3 (Android 9)",
            false,
            Optional.empty())
    );
  }

  @ParameterizedTest
  @MethodSource
  void getAdditionalSpecifierTags(@Nullable final String userAgent, final Tags expectedTags) {
    assertEquals(expectedTags, UserAgentTagUtil.getAdditionalSpecifierTags(userAgent));
  }

  private static List<Arguments> getAdditionalSpecifierTags() {
    return List.of(
        Arguments.argumentSet("null UA", null, Tags.empty()),
        Arguments.argumentSet("nonsense UA", "This is not a valid User-Agent string", Tags.empty()),
        Arguments.argumentSet("no additional specifiers", "ZonaRosa-Desktop/7.84.0", Tags.empty()),
        Arguments.argumentSet("nonstandard additional specifiers", "ZonaRosa-Desktop/7.84.0 superfluous information", Tags.empty()),
        Arguments.argumentSet("desktop standard additional specifiers", "ZonaRosa-Desktop/7.84.0 macOS 21.6.0 libzonarosa/0.86.3",
            Tags.of(
                UserAgentTagUtil.OPERATING_SYSTEM_TAG, "macOS",
                UserAgentTagUtil.OPERATING_SYSTEM_VERSION_TAG, "21.6.0",
                UserAgentTagUtil.LIBZONAROSA_VERSION_TAG, "0.86.3")),
        Arguments.argumentSet("android standard additional specifiers", "ZonaRosa-Android/7.63.3 Android/34 libzonarosa/0.85.1",
            Tags.of(
                UserAgentTagUtil.OPERATING_SYSTEM_TAG, "Android",
                UserAgentTagUtil.OPERATING_SYSTEM_VERSION_TAG, "34",
                UserAgentTagUtil.LIBZONAROSA_VERSION_TAG, "0.85.1")),
        Arguments.argumentSet("ios standard additional specifiers", "ZonaRosa-iOS/7.89.0.1253 iOS/26.1 libzonarosa/0.86.7",
            Tags.of(
                UserAgentTagUtil.OPERATING_SYSTEM_TAG, "iOS",
                UserAgentTagUtil.OPERATING_SYSTEM_VERSION_TAG, "26.1",
                UserAgentTagUtil.LIBZONAROSA_VERSION_TAG, "0.86.7"))
    );
  }
}
