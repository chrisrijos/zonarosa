/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.configuration.secrets;

import com.google.common.collect.ImmutableList;
import jakarta.validation.constraints.NotEmpty;
import java.util.Collection;
import java.util.List;
import org.hibernate.validator.internal.constraintvalidators.bv.notempty.NotEmptyValidatorForCollection;

public class SecretStringList extends Secret<List<String>> {

  @SuppressWarnings("rawtypes")
  public static class ValidatorNotEmpty extends BaseSecretValidator<NotEmpty, Collection, SecretStringList> {
    public ValidatorNotEmpty() {
      super(new NotEmptyValidatorForCollection());
    }
  }

  public SecretStringList(final List<String> value) {
    super(ImmutableList.copyOf(value));
  }
}
