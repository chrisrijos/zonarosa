/*
 * Copyright 2013-2022 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Duration;
import java.time.Instant;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import io.zonarosa.server.util.SystemMapper;

class DeviceTest {

  @ParameterizedTest
  @CsvSource({
      "true, P1D, false",
      "true, P30D, false",
      "true, P31D, false",
      "true, P180D, false",
      "true, P181D, true",
      "false, P1D, false",
      "false, P45D, false",
      "false, P46D, true",
      "false, P180D, true",
  })
  public void testIsExpired(final boolean primary, final Duration timeSinceLastSeen, final boolean expectExpired) {

    final long lastSeen = Instant.now()
        .minus(timeSinceLastSeen)
        // buffer for test runtime
        .plusSeconds(1)
        .toEpochMilli();

    final Device device = new Device();
    device.setId(primary ? Device.PRIMARY_ID : Device.PRIMARY_ID + 1);
    device.setCreated(lastSeen);
    device.setLastSeen(lastSeen);

    assertEquals(expectExpired, device.isExpired());
  }

  @Test
  void deserializeCapabilities() throws JsonProcessingException {
    {
      final Device device = SystemMapper.jsonMapper().readValue("""
          {
            "capabilities": null
          }
          """, Device.class);

      assertNotNull(device.getCapabilities(),
          "Device deserialization should populate null capabilities with an empty set");
    }

    {
      final Device device = SystemMapper.jsonMapper().readValue("{}", Device.class);

      assertNotNull(device.getCapabilities(),
          "Device deserialization should populate null capabilities with an empty set");
    }
  }

}
