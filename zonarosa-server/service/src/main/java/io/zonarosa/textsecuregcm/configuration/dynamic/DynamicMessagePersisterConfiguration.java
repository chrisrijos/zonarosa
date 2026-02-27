/*
 * Copyright 2022 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.configuration.dynamic;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.VisibleForTesting;

public class DynamicMessagePersisterConfiguration {

  @JsonProperty
  private boolean persistenceEnabled = true;

  /**
   * If we have to trim a client's persisted queue to make room to persist from Redis to DynamoDB, how much extra room should we make
   */
  @JsonProperty
  private double trimOversizedQueueExtraRoomRatio = 1.5;

  public DynamicMessagePersisterConfiguration() {}

  @VisibleForTesting
  public DynamicMessagePersisterConfiguration(final boolean persistenceEnabled, final double trimOversizedQueueExtraRoomRatio) {
    this.persistenceEnabled = persistenceEnabled;
    this.trimOversizedQueueExtraRoomRatio = trimOversizedQueueExtraRoomRatio;
  }

  public boolean isPersistenceEnabled() {
    return persistenceEnabled;
  }

  public double getTrimOversizedQueueExtraRoomRatio() {
    return trimOversizedQueueExtraRoomRatio;
  }

}
