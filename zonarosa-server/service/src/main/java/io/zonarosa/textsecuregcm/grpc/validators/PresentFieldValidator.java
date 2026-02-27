/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.grpc.validators;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import java.util.Set;

public class PresentFieldValidator extends BaseFieldValidator<Boolean> {

  public PresentFieldValidator() {
    super("present",
        Set.of(Descriptors.FieldDescriptor.Type.MESSAGE),
        MissingOptionalAction.FAIL,
        true);
  }

  @Override
  protected Boolean resolveExtensionValue(final Object extensionValue) throws FieldValidationException {
    return requireFlagExtension(extensionValue);
  }

  @Override
  protected void validateMessageValue(final Boolean extensionValue, final Message msg) throws FieldValidationException {
    if (msg == null) {
      throw new FieldValidationException("message expected to be present");
    }
  }
}
