//
// Copyright 2014-2016 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.protocol.state;

import io.zonarosa.libzonarosa.protocol.groups.state.SenderKeyStore;

public interface ZonaRosaProtocolStore
    extends IdentityKeyStore,
        PreKeyStore,
        SessionStore,
        SignedPreKeyStore,
        SenderKeyStore,
        KyberPreKeyStore {}
