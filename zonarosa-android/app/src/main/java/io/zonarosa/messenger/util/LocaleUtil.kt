package io.zonarosa.messenger.util

import androidx.core.os.LocaleListCompat
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.util.dynamiclanguage.LanguageString
import java.util.Locale

object LocaleUtil {

  fun getFirstLocale(): Locale {
    return getLocaleDefaults().firstOrNull() ?: Locale.getDefault()
  }

  /**
   * Get a user priority list of locales supported on the device, with the locale set via ZonaRosa settings
   * as highest priority over system settings.
   */
  fun getLocaleDefaults(): List<Locale> {
    val locales: MutableList<Locale> = mutableListOf()
    val zonarosaLocale: Locale? = LanguageString.parseLocale(ZonaRosaStore.settings.language)
    val localeList: LocaleListCompat = LocaleListCompat.getDefault()

    if (zonarosaLocale != null) {
      locales += zonarosaLocale
    }

    for (index in 0 until localeList.size()) {
      locales += localeList.get(index) ?: continue
    }

    return locales
  }
}
