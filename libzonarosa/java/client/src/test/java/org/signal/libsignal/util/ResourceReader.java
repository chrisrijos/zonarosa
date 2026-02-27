//
// Copyright 2023 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ResourceReader {
  public static byte[] readAll(final InputStream inputStream) throws IOException {
    try {
      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      final byte[] chunk = new byte[4096];
      int read;
      while ((read = inputStream.read(chunk, 0, chunk.length)) != -1) {
        baos.write(chunk, 0, read);
      }
      return baos.toByteArray();
    } finally {
      inputStream.close();
    }
  }

  public static int getLength(final InputStream inputStream) throws IOException {
    int length = 0;
    try (InputStream is = inputStream) {
      final byte[] chunk = new byte[4096];
      int read;

      while ((read = inputStream.read(chunk, 0, chunk.length)) != -1) {
        length += read;
      }
    }
    return length;
  }
}
