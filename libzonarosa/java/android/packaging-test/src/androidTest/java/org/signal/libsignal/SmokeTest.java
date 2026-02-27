//
// Copyright 2025 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa;

import static org.junit.Assert.assertThrows;

import org.junit.Test;
import io.zonarosa.libzonarosa.internal.Native;
import io.zonarosa.libzonarosa.internal.NativeTesting;

/**
 * Tests that check that libzonarosa is loadable and available.
 *
 * <p>This test is expected to run in the production configuration, where only {@code
 * libzonarosa_jni.so} is available (as opposed to the test/debug configuration, which also includes
 * {@code libzonarosa_jni_testing.so}). The difference is important, since when both are available,
 * {@code libzonarosa_jni_testing.so} is loaded, with <code>libzonarosa_jni.so</code> as a fallback.
 * {@code libzonarosa_jni_testing.so} exposes a superset of the <code>
 * libzonarosa_jni.so</code> API, including some test only functions, but the actual production
 * configuration only loads {@code libzonarosa_jni.so}. These tests check that the custom loading code
 * works correctly in production configurations in addition to the test/debug configuration.
 */
public class SmokeTest {
  @Test
  public void testCanCallNativeMethod() {
    Native.keepAlive(null);
  }

  @Test
  public void testCantCallNativeTestingMethod() {
    assertThrows(UnsatisfiedLinkError.class, () -> NativeTesting.test_only_fn_returns_123());
  }
}
