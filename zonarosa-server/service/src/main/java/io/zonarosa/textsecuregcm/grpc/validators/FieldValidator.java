/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.grpc.validators;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;

public interface FieldValidator {

  void validate(Object extensionValue, Descriptors.FieldDescriptor fd, Message msg)
      throws FieldValidationException;
}
