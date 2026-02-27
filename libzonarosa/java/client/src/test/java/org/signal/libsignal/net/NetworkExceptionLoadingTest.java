//
// Copyright 2025 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.net;

import static org.junit.Assert.*;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import io.zonarosa.libzonarosa.internal.TokioAsyncContext;

public class NetworkExceptionLoadingTest {
  @Test
  public void loadExceptionClasses() throws ExecutionException, InterruptedException {
    TokioAsyncContext context = new TokioAsyncContext();
    assertCanLoadClass(context, "io.zonarosa.libzonarosa.net.CdsiProtocolException");
    assertCanLoadClass(context, "io.zonarosa.libzonarosa.net.NetworkException");
  }

  @Test
  public void loadNonexistentClasses() throws ExecutionException, InterruptedException {
    TokioAsyncContext context = new TokioAsyncContext();
    assertClassNotFound(context, "io.zonarosa.libzonarosa.ClassThatDoesNotExist1");
    assertClassNotFound(context, "io.zonarosa.libzonarosa.ClassThatDoesNotExist2");
    assertClassNotFound(context, "io.zonarosa.libzonarosa.ClassThatDoesNotExist3");
    assertClassNotFound(context, "io.zonarosa.libzonarosa.ClassThatDoesNotExist4");
    assertClassNotFound(context, "io.zonarosa.libzonarosa.ClassThatDoesNotExist5");
    assertClassNotFound(context, "io.zonarosa.libzonarosa.ClassThatDoesNotExist6");
    assertClassNotFound(context, "io.zonarosa.libzonarosa.ClassThatDoesNotExist7");
    assertClassNotFound(context, "io.zonarosa.libzonarosa.ClassThatDoesNotExist8");
    assertClassNotFound(context, "io.zonarosa.libzonarosa.ClassThatDoesNotExist9");
    assertClassNotFound(context, "io.zonarosa.libzonarosa.ClassThatDoesNotExist10");
  }

  @Test
  public void runNetworkClassLoadTestFunction() throws ExecutionException, InterruptedException {
    Network.checkClassesCanBeLoadedAsyncForTest();
  }

  /** Assert that the class with the given name can be loaded on a Tokio worker thread. */
  private static void assertCanLoadClass(TokioAsyncContext context, String className)
      throws ExecutionException, InterruptedException {
    Future<Class<Object>> loadAsync = context.loadClassAsync(className);
    // Block waiting for the future to resolve.
    Class<Object> loaded = loadAsync.get();
    assertEquals(className, loaded.getName());
  }

  /** Assert that the class doesn't exist. */
  private static void assertClassNotFound(TokioAsyncContext context, String className)
      throws ExecutionException, InterruptedException {
    Future<Class<Object>> loadAsync = context.loadClassAsync(className);
    // Block waiting for the future to resolve.
    Throwable cause =
        assertThrows(
                "for " + className,
                ExecutionException.class,
                () -> loadAsync.get(10, TimeUnit.SECONDS))
            .getCause();
    assertTrue(
        "unexpected error: " + cause,
        cause instanceof ClassNotFoundException || cause instanceof NoClassDefFoundError);
  }
}
