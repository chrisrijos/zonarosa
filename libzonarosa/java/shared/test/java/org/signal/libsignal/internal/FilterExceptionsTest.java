//
// Copyright 2024 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.media;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static io.zonarosa.libzonarosa.internal.FilterExceptions.filterExceptions;

import org.junit.Test;
import io.zonarosa.libzonarosa.internal.FilterExceptions.ThrowingNativeVoidOperation;

public class FilterExceptionsTest {

  private static class UnexpectedException extends Exception {
    public UnexpectedException(String message) {
      super(message);
    }
  }

  @Test
  public void exceptionTextIncludesClass() {
    AssertionError error =
        assertThrows(
            AssertionError.class,
            () -> {
              filterExceptions(
                  (ThrowingNativeVoidOperation)
                      () -> {
                        throw new UnexpectedException("not expected");
                      });
            });

    assertThat(error.getMessage(), containsString("FilterExceptionsTest$UnexpectedException"));
  }
}
