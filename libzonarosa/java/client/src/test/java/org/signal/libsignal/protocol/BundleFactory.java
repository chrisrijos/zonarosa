//
// Copyright 2023 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.protocol;

import io.zonarosa.libzonarosa.protocol.state.PreKeyBundle;
import io.zonarosa.libzonarosa.protocol.state.ZonaRosaProtocolStore;

public interface BundleFactory {
  PreKeyBundle createBundle(ZonaRosaProtocolStore store) throws InvalidKeyException;
}
