/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.subscriptions;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;
import javax.annotation.Nonnull;
import io.zonarosa.i18n.HeaderControlledResourceBundleLookup;

public class BankMandateTranslator {
  private static final String BASE_NAME = "io.zonarosa.bankmandate.BankMandate";
  private final HeaderControlledResourceBundleLookup headerControlledResourceBundleLookup;

  public BankMandateTranslator(
      @Nonnull final HeaderControlledResourceBundleLookup headerControlledResourceBundleLookup) {
    this.headerControlledResourceBundleLookup = Objects.requireNonNull(headerControlledResourceBundleLookup);
  }

  public String translate(final List<Locale> acceptableLanguages, final BankTransferType bankTransferType) {
    final ResourceBundle resourceBundle = headerControlledResourceBundleLookup.getResourceBundle(BASE_NAME,
        acceptableLanguages);
    return resourceBundle.getString(getKey(bankTransferType));
  }

  private static String getKey(final BankTransferType bankTransferType) {
    return switch (bankTransferType) {
      case SEPA_DEBIT -> "SEPA_MANDATE";
    };
  }
}
