//
// Copyright 2023 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.protocol;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;

import java.util.UUID;
import org.junit.Test;

public class ProtocolAddressTest {
  @Test
  public void testRoundTripServiceId() {
    UUID uuid = UUID.randomUUID();
    ServiceId aci = new ServiceId.Aci(uuid);
    ServiceId pni = new ServiceId.Pni(uuid);

    ZonaRosaProtocolAddress aciAddr = new ZonaRosaProtocolAddress(aci, 1);
    ZonaRosaProtocolAddress pniAddr = new ZonaRosaProtocolAddress(pni, 1);
    assertNotEquals(aciAddr, pniAddr);
    assertEquals(aci, aciAddr.getServiceId());
    assertEquals(pni, pniAddr.getServiceId());
  }

  @Test
  public void testInvalidDeviceId() {
    UUID uuid = UUID.randomUUID();
    ServiceId aci = new ServiceId.Aci(uuid);

    var exception =
        assertThrows(IllegalArgumentException.class, () -> new ZonaRosaProtocolAddress(aci, 1234));

    assertThat(exception.getMessage(), containsString(aci.toServiceIdString()));
    assertThat(exception.getMessage(), containsString("1234"));
  }
}
