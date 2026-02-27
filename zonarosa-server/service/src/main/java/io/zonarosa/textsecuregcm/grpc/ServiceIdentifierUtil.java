/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.grpc;

import com.google.protobuf.ByteString;
import io.grpc.Status;
import java.util.UUID;
import io.zonarosa.server.identity.AciServiceIdentifier;
import io.zonarosa.server.identity.PniServiceIdentifier;
import io.zonarosa.server.identity.ServiceIdentifier;
import io.zonarosa.server.util.UUIDUtil;

public class ServiceIdentifierUtil {

  private ServiceIdentifierUtil() {
  }

  public static ServiceIdentifier fromGrpcServiceIdentifier(final io.zonarosa.chat.common.ServiceIdentifier serviceIdentifier) {
    final UUID uuid;

    try {
      uuid = UUIDUtil.fromByteString(serviceIdentifier.getUuid());
    } catch (final IllegalArgumentException e) {
      throw GrpcExceptions.invalidArguments("invalid service identifier");
    }

    return switch (IdentityTypeUtil.fromGrpcIdentityType(serviceIdentifier.getIdentityType())) {
      case ACI -> new AciServiceIdentifier(uuid);
      case PNI -> new PniServiceIdentifier(uuid);
    };
  }

  public static io.zonarosa.chat.common.ServiceIdentifier toGrpcServiceIdentifier(final ServiceIdentifier serviceIdentifier) {
    return io.zonarosa.chat.common.ServiceIdentifier.newBuilder()
        .setIdentityType(IdentityTypeUtil.toGrpcIdentityType(serviceIdentifier.identityType()))
        .setUuid(UUIDUtil.toByteString(serviceIdentifier.uuid()))
        .build();
  }

  public static ByteString toCompactByteString(final ServiceIdentifier serviceIdentifier) {
    return ByteString.copyFrom(serviceIdentifier.toCompactByteArray());
  }

  public static ServiceIdentifier fromByteString(final ByteString byteString) {
    return ServiceIdentifier.fromBytes(byteString.toByteArray());
  }
}
