//
// Copyright 2025 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.protocol;

import io.zonarosa.libzonarosa.internal.NativeHandleGuard;

/**
 * Marker interface for public key types compatible with native code.
 *
 * <p>This is only implemented by {@link io.zonarosa.libzonarosa.protocol.ecc.ECPublicKey} and {@link
 * io.zonarosa.libzonarosa.protocol.kem.KEMPublicKey}. If that changes, the corresponding Rust
 * conversion code should be updated.
 */
public interface SerializablePublicKey extends NativeHandleGuard.Owner {}
