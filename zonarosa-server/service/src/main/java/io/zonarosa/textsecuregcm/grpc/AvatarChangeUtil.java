/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.grpc;

import io.grpc.Status;
import io.zonarosa.server.entities.AvatarChange;

public class AvatarChangeUtil {
  public static AvatarChange fromGrpcAvatarChange(final io.zonarosa.chat.profile.SetProfileRequest.AvatarChange avatarChangeType) {
    return switch (avatarChangeType) {
      case AVATAR_CHANGE_UNCHANGED -> AvatarChange.AVATAR_CHANGE_UNCHANGED;
      case AVATAR_CHANGE_CLEAR -> AvatarChange.AVATAR_CHANGE_CLEAR;
      case AVATAR_CHANGE_UPDATE -> AvatarChange.AVATAR_CHANGE_UPDATE;
      case UNRECOGNIZED -> throw Status.INVALID_ARGUMENT.withDescription("Invalid avatar change value").asRuntimeException();
    };
  }
}
