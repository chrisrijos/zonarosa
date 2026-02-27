//
// Copyright 2024 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.internal;

public class TokioAsyncContext extends NativeHandleGuard.SimpleOwner {
  public TokioAsyncContext() {
    super(Native.TokioAsyncContext_new());
  }

  // For testing
  TokioAsyncContext(long rawHandle) {
    super(rawHandle);
  }

  @SuppressWarnings("unchecked")
  public CompletableFuture<Class<Object>> loadClassAsync(String className) {
    return (CompletableFuture<Class<Object>>) Native.AsyncLoadClass(this, className);
  }

  @Override
  protected void release(final long nativeHandle) {
    Native.TokioAsyncContext_Destroy(nativeHandle);
  }
}
