/*
 * Copyright 2021 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.badges;

import java.util.List;
import java.util.Locale;

public interface LevelTranslator {
  String translate(List<Locale> acceptableLanguages, String badgeId);
}
