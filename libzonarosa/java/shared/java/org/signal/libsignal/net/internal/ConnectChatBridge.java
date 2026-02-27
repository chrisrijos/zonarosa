//
// Copyright 2025 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.net.internal;

import io.zonarosa.libzonarosa.internal.CalledFromNative;

/** A helper interface that represents the callback methods used by the Rust side of the bridge. */
@CalledFromNative
public interface ConnectChatBridge {
  long getConnectionManagerUnsafeNativeHandle();
}
