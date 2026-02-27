package io.zonarosa.messenger.payments;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.zonarosa.core.util.logging.Log;
import io.zonarosa.messenger.util.RemoteConfig;
import io.zonarosa.core.util.Util;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class GeographicalRestrictions {

  private static final String TAG = Log.tag(GeographicalRestrictions.class);

  private GeographicalRestrictions() {}

  public static boolean e164Allowed(@Nullable String e164) {
    if (e164 == null) {
      return false;
    }

    String bareE164 = e164.startsWith("+") ? e164.substring(1) : e164;

    return parsePrefixes(RemoteConfig.paymentsCountryBlocklist())
        .stream()
        .noneMatch(bareE164::startsWith);
  }

  private static List<String> parsePrefixes(@NonNull String serializedList) {
    return Arrays.stream(serializedList.split(","))
        .map(v -> v.replaceAll(" ", ""))
        .map(String::trim)
        .filter(v -> !Util.isEmpty(v))
        .collect(Collectors.toList());
  }
}
