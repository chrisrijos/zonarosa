/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.grpc.validators;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import io.grpc.ServerServiceDefinition;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.protobuf.ProtoServiceDescriptorSupplier;
import java.util.Map;
import java.util.Optional;
import io.zonarosa.chat.require.Auth;

public final class ValidatorUtils {

  public static final String REQUIRE_AUTH_EXTENSION_NAME = "io.zonarosa.chat.require.auth";

  private ValidatorUtils() {
    // noop
  }

  public static Optional<Auth> serviceAuthExtensionValue(final ServerServiceDefinition serviceDefinition) {
    return serviceExtensionValueByName(serviceDefinition, REQUIRE_AUTH_EXTENSION_NAME)
        .map(val -> Auth.valueOf((Descriptors.EnumValueDescriptor) val));
  }

  private static Optional<Object> serviceExtensionValueByName(
      final ServerServiceDefinition serviceDefinition,
      final String fullExtensionName) {
    final Object schemaDescriptor = serviceDefinition.getServiceDescriptor().getSchemaDescriptor();
    if (schemaDescriptor instanceof ProtoServiceDescriptorSupplier protoServiceDescriptorSupplier) {
      final DescriptorProtos.ServiceOptions options = protoServiceDescriptorSupplier.getServiceDescriptor().getOptions();
      return options.getAllFields().entrySet()
          .stream()
          .filter(e -> e.getKey().getFullName().equals(fullExtensionName))
          .map(Map.Entry::getValue)
          .findFirst();
    }
    return Optional.empty();
  }
}
