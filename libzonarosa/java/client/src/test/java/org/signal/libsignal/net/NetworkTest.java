//
// Copyright 2024 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.net;

import org.junit.Test;

public class NetworkTest {
  private static final String USER_AGENT = "test";

  @Test
  public void networkChange() {
    // There's no feedback from this, we're just making sure it doesn't normally crash or throw.
    var net = new Network(Network.Environment.STAGING, USER_AGENT);
    net.onNetworkChange();
  }
}
