/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.configuration.secrets;

import static java.util.Objects.requireNonNull;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.annotation.Annotation;

public abstract class BaseSecretValidator<A extends Annotation, T, S extends Secret<? extends T>> implements ConstraintValidator<A, S> {

  private final ConstraintValidator<A, T> validator;


  protected BaseSecretValidator(final ConstraintValidator<A, T> validator) {
    this.validator = requireNonNull(validator);
  }

  @Override
  public void initialize(final A constraintAnnotation) {
    validator.initialize(constraintAnnotation);
  }

  @Override
  public boolean isValid(final S value, final ConstraintValidatorContext context) {
    return validator.isValid(value.value(), context);
  }
}
