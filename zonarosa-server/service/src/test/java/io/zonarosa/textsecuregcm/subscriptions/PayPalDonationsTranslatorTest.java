/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.subscriptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import org.junit.jupiter.api.Test;
import io.zonarosa.i18n.HeaderControlledResourceBundleLookup;

class PayPalDonationsTranslatorTest {

  private final PayPalDonationsTranslator translator = new PayPalDonationsTranslator(
      new HeaderControlledResourceBundleLookup());

  @Test
  void testTranslate() {
    assertEquals("Donation to ZonaRosa Technology Foundation",
        translator.translate(List.of(Locale.ROOT), PayPalDonationsTranslator.ONE_TIME_DONATION_LINE_ITEM_KEY));
  }

  @Test
  void testTranslateUnknownKey() {
    assertThrows(MissingResourceException.class, () -> translator.translate(List.of(Locale.ROOT), "unknown-key"));
  }

}
