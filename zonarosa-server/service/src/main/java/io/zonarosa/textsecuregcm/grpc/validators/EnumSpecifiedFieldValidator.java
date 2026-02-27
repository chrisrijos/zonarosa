/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.grpc.validators;

import com.google.protobuf.Descriptors;
import java.util.Set;

public class EnumSpecifiedFieldValidator extends BaseFieldValidator<Boolean> {

  public EnumSpecifiedFieldValidator() {
    super("specified", Set.of(Descriptors.FieldDescriptor.Type.ENUM), MissingOptionalAction.FAIL, false);
  }

  @Override
  protected Boolean resolveExtensionValue(final Object extensionValue) throws FieldValidationException {
    return requireFlagExtension(extensionValue);
  }

  @Override
  protected void validateEnumValue(
      final Boolean extensionValue,
      final Descriptors.EnumValueDescriptor enumValueDescriptor) throws FieldValidationException {
    if (enumValueDescriptor.getIndex() <= 0) {
      throw new FieldValidationException("enum field must be specified");
    }
  }
}
