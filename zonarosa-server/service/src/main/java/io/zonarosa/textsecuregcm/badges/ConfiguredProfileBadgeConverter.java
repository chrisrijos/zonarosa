/*
 * Copyright 2021 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.badges;

import com.google.common.annotations.VisibleForTesting;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.stream.Collectors;
import io.zonarosa.i18n.HeaderControlledResourceBundleLookup;
import io.zonarosa.server.configuration.BadgeConfiguration;
import io.zonarosa.server.configuration.BadgesConfiguration;
import io.zonarosa.server.entities.Badge;
import io.zonarosa.server.entities.BadgeSvg;
import io.zonarosa.server.entities.SelfBadge;
import io.zonarosa.server.storage.AccountBadge;

public class ConfiguredProfileBadgeConverter implements ProfileBadgeConverter, BadgeTranslator {

  @VisibleForTesting
  static final String BASE_NAME = "io.zonarosa.badges.Badges";

  private final Clock clock;
  private final Map<String, BadgeConfiguration> knownBadges;
  private final List<String> badgeIdsEnabledForAll;
  private final HeaderControlledResourceBundleLookup headerControlledResourceBundleLookup;

  public ConfiguredProfileBadgeConverter(
      final Clock clock,
      final BadgesConfiguration badgesConfiguration,
      final HeaderControlledResourceBundleLookup headerControlledResourceBundleLookup) {
    this.clock = clock;
    this.knownBadges = badgesConfiguration.getBadges().stream()
        .collect(Collectors.toMap(BadgeConfiguration::getId, Function.identity()));
    this.badgeIdsEnabledForAll = badgesConfiguration.getBadgeIdsEnabledForAll();
    this.headerControlledResourceBundleLookup = headerControlledResourceBundleLookup;
  }

  @Override
  public Badge translate(final List<Locale> acceptableLanguages, final String badgeId) {
    final ResourceBundle resourceBundle = headerControlledResourceBundleLookup.getResourceBundle(BASE_NAME,
        acceptableLanguages);
    final BadgeConfiguration configuration = knownBadges.get(badgeId);
    return newBadge(
        false,
        configuration.getId(),
        configuration.getCategory(),
        resourceBundle.getString(configuration.getId() + "_name"),
        resourceBundle.getString(configuration.getId() + "_description"),
        configuration.getSprites(),
        configuration.getSvg(),
        configuration.getSvgs(),
        null,
        false);
  }

  @Override
  public List<Badge> convert(
      final List<Locale> acceptableLanguages,
      final List<AccountBadge> accountBadges,
      final boolean isSelf) {
    if (accountBadges.isEmpty() && badgeIdsEnabledForAll.isEmpty()) {
      return List.of();
    }

    final Instant now = clock.instant();
    final ResourceBundle resourceBundle = headerControlledResourceBundleLookup.getResourceBundle(BASE_NAME,
        acceptableLanguages);
    List<Badge> badges = accountBadges.stream()
        .filter(accountBadge -> (isSelf || accountBadge.visible())
            && now.isBefore(accountBadge.expiration())
            && knownBadges.containsKey(accountBadge.id()))
        .map(accountBadge -> {
          BadgeConfiguration configuration = knownBadges.get(accountBadge.id());
          return newBadge(
              isSelf,
              accountBadge.id(),
              configuration.getCategory(),
              resourceBundle.getString(accountBadge.id() + "_name"),
              resourceBundle.getString(accountBadge.id() + "_description"),
              configuration.getSprites(),
              configuration.getSvg(),
              configuration.getSvgs(),
              accountBadge.expiration(),
              accountBadge.visible());
        })
        .collect(Collectors.toCollection(ArrayList::new));
    badges.addAll(badgeIdsEnabledForAll.stream().filter(knownBadges::containsKey).map(id -> {
      BadgeConfiguration configuration = knownBadges.get(id);
      return newBadge(
          isSelf,
          id,
          configuration.getCategory(),
          resourceBundle.getString(id + "_name"),
          resourceBundle.getString(id + "_description"),
          configuration.getSprites(),
          configuration.getSvg(),
          configuration.getSvgs(),
          now.plus(Duration.ofDays(1)),
          true);
    }).collect(Collectors.toList()));
    return badges;
  }

  private Badge newBadge(
      final boolean isSelf,
      final String id,
      final String category,
      final String name,
      final String description,
      final List<String> sprites,
      final String svg,
      final List<BadgeSvg> svgs,
      final Instant expiration,
      final boolean visible) {
    if (isSelf) {
      return new SelfBadge(id, category, name, description, sprites, svg, svgs, expiration, visible);
    } else {
      return new Badge(id, category, name, description, sprites, svg, svgs);
    }
  }
}
