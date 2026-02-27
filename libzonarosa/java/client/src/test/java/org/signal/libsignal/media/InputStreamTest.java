//
// Copyright 2024 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.media;

import static org.junit.Assert.assertArrayEquals;

import java.io.ByteArrayInputStream;
import org.junit.Test;
import io.zonarosa.libzonarosa.internal.NativeTesting;

public class InputStreamTest {

  @Test
  public void testReadIntoEmptyBuffer() {
    byte[] data = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".getBytes();
    assertArrayEquals(
        NativeTesting.TESTING_InputStreamReadIntoZeroLengthSlice(new ByteArrayInputStream(data)),
        data);
  }
}
