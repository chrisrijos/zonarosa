/*
 * Copyright 2021 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.entities;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;

public record GroupCredentials(List<GroupCredential> credentials, List<CallLinkAuthCredential> callLinkAuthCredentials, @Nullable UUID pni) {

  public record GroupCredential(byte[] credential, long redemptionTime) {
  }

  public record CallLinkAuthCredential(byte[] credential, long redemptionTime) {
  }
}
