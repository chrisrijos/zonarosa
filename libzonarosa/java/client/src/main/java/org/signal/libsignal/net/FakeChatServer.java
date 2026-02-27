//
// Copyright 2025 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.net;

import io.zonarosa.libzonarosa.internal.CompletableFuture;
import io.zonarosa.libzonarosa.internal.NativeHandleGuard;
import io.zonarosa.libzonarosa.internal.NativeTesting;
import io.zonarosa.libzonarosa.internal.TokioAsyncContext;

class FakeChatServer extends NativeHandleGuard.SimpleOwner {
  private TokioAsyncContext tokioContext;

  public FakeChatServer(TokioAsyncContext tokioContext) {
    this(tokioContext, NativeTesting.TESTING_FakeChatServer_Create());
  }

  private FakeChatServer(TokioAsyncContext tokioContext, long nativeHandle) {
    super(nativeHandle);
    this.tokioContext = tokioContext;
  }

  public TokioAsyncContext getTokioContext() {
    return this.tokioContext;
  }

  public CompletableFuture<FakeChatRemote> getNextRemote() {
    return tokioContext
        .guardedMap(
            asyncContextHandle ->
                this.guardedMap(
                    fakeServer ->
                        NativeTesting.TESTING_FakeChatServer_GetNextRemote(
                            asyncContextHandle, fakeServer)))
        .thenApply(fakeRemote -> new FakeChatRemote(tokioContext, fakeRemote));
  }

  @Override
  protected void release(long nativeHandle) {
    NativeTesting.FakeChatServer_Destroy(nativeHandle);
  }
}
