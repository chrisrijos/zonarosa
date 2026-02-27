/*
 * Copyright 2013-2020 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.util;

import io.dropwizard.util.DataSize;

public class Constants {
  public static final String METRICS_NAME = "zonarosa";
  public static final int MAXIMUM_STICKER_SIZE_BYTES = (int) DataSize.kibibytes(300 + 1).toBytes(); // add 1 kiB for encryption overhead
  public static final int MAXIMUM_STICKER_MANIFEST_SIZE_BYTES = (int) DataSize.kibibytes(10).toBytes();
}
