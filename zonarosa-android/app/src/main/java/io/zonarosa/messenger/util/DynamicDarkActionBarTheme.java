package io.zonarosa.messenger.util;

import androidx.annotation.StyleRes;

import io.zonarosa.messenger.R;

public class DynamicDarkActionBarTheme extends DynamicTheme {

  protected @StyleRes int getTheme() {
    return R.style.ZonaRosa_DayNight_DarkActionBar;
  }
}
