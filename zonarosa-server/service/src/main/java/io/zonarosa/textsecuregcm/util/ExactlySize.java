/*
 * Copyright 2013 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.util;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target({ FIELD, METHOD, CONSTRUCTOR, PARAMETER, ANNOTATION_TYPE, TYPE_USE })
@Retention(RUNTIME)
@Constraint(validatedBy = {
    ExactlySizeValidatorForString.class,
    ExactlySizeValidatorForArraysOfByte.class,
    ExactlySizeValidatorForCollection.class,
    ExactlySizeValidatorForSecretBytes.class,
})
@Documented
public @interface ExactlySize {

  String message() default "{io.zonarosa.server.util.ExactlySize.message}";

  Class<?>[] groups() default { };

  Class<? extends Payload>[] payload() default { };

  int[] value();

  @Target({ FIELD, METHOD, PARAMETER, ANNOTATION_TYPE })
  @Retention(RUNTIME)
  @Documented
  @interface List {
    ExactlySize[] value();
  }
}
