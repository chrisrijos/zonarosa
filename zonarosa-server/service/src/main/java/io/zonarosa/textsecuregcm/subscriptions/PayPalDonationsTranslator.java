/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.subscriptions;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;
import javax.annotation.Nonnull;
import io.zonarosa.i18n.HeaderControlledResourceBundleLookup;

public class PayPalDonationsTranslator {

  public static final String ONE_TIME_DONATION_LINE_ITEM_KEY = "oneTime.donationLineItemName";

  private static final String BASE_NAME = "io.zonarosa.donations.PayPal";

  private final HeaderControlledResourceBundleLookup headerControlledResourceBundleLookup;

  public PayPalDonationsTranslator(
      @Nonnull final HeaderControlledResourceBundleLookup headerControlledResourceBundleLookup) {
    this.headerControlledResourceBundleLookup = Objects.requireNonNull(headerControlledResourceBundleLookup);
  }

  public String translate(final List<Locale> acceptableLanguages, final String key) {
    final ResourceBundle resourceBundle = headerControlledResourceBundleLookup.getResourceBundle(BASE_NAME,
        acceptableLanguages);
    return resourceBundle.getString(key);
  }
}
