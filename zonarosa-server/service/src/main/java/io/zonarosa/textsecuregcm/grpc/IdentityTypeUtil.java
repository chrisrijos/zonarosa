/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.grpc;

import io.grpc.Status;
import io.zonarosa.server.identity.IdentityType;

public class IdentityTypeUtil {

  private IdentityTypeUtil() {
  }

  public static IdentityType fromGrpcIdentityType(final io.zonarosa.chat.common.IdentityType grpcIdentityType) {
    return switch (grpcIdentityType) {
      case IDENTITY_TYPE_ACI -> IdentityType.ACI;
      case IDENTITY_TYPE_PNI -> IdentityType.PNI;
      case IDENTITY_TYPE_UNSPECIFIED, UNRECOGNIZED -> throw GrpcExceptions.invalidArguments("invalid identity type");
    };
  }

  public static io.zonarosa.chat.common.IdentityType toGrpcIdentityType(final IdentityType identityType) {
    return switch (identityType) {
      case ACI -> io.zonarosa.chat.common.IdentityType.IDENTITY_TYPE_ACI;
      case PNI -> io.zonarosa.chat.common.IdentityType.IDENTITY_TYPE_PNI;
    };
  }
}
