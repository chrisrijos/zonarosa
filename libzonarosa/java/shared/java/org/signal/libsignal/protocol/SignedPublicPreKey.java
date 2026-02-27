//
// Copyright 2025 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.protocol;

/**
 * The public parts of a {@link io.zonarosa.libzonarosa.protocol.state.SignedPreKeyRecord} or {@link
 * io.zonarosa.libzonarosa.protocol.state.KyberPreKeyRecord}.
 *
 * <p>This is what gets uploaded when setting pre-keys while registering an account.
 */
public record SignedPublicPreKey<Key extends SerializablePublicKey>(
    int id, Key publicKey, byte[] signature) {}
