/*
 * Copyright 2021 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.badges;

import java.util.List;
import java.util.Locale;
import io.zonarosa.server.entities.Badge;
import io.zonarosa.server.storage.AccountBadge;

public interface ProfileBadgeConverter {

  /**
   * Converts the {@link AccountBadge}s for an account into the objects
   * that can be returned on a profile fetch.
   */
  List<Badge> convert(List<Locale> acceptableLanguages, List<AccountBadge> accountBadges, boolean isSelf);
}
