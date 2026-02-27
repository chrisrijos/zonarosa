/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.tests.util;

import io.zonarosa.server.configuration.dynamic.DynamicConfiguration;
import io.zonarosa.server.configuration.dynamic.DynamicExperimentEnrollmentConfiguration;
import io.zonarosa.server.experiment.ExperimentEnrollmentManager;
import io.zonarosa.server.storage.DynamicConfigurationManager;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExperimentHelper {

  private static DynamicConfigurationManager<DynamicConfiguration> withEnrollment(
      final String experimentName,
      final Set<UUID> enrolledUuids,
      final int enrollmentPercentage) {
    final DynamicConfigurationManager<DynamicConfiguration> dcm = mock(DynamicConfigurationManager.class);
    final DynamicConfiguration dc = mock(DynamicConfiguration.class);
    when(dcm.getConfiguration()).thenReturn(dc);
    final DynamicExperimentEnrollmentConfiguration exp = mock(DynamicExperimentEnrollmentConfiguration.class);
    when(dc.getExperimentEnrollmentConfiguration(experimentName)).thenReturn(Optional.of(exp));
    final DynamicExperimentEnrollmentConfiguration.UuidSelector uuidSelector =
        mock(DynamicExperimentEnrollmentConfiguration.UuidSelector.class);
    when(exp.getUuidSelector()).thenReturn(uuidSelector);

    when(exp.getEnrollmentPercentage()).thenReturn(enrollmentPercentage);
    when(uuidSelector.getUuids()).thenReturn(enrolledUuids);
    when(uuidSelector.getUuidEnrollmentPercentage()).thenReturn(100);
    return dcm;
  }

  public static ExperimentEnrollmentManager withEnrollment(final String experimentName, final Set<UUID> enrolledUuids) {
    return new ExperimentEnrollmentManager(withEnrollment(experimentName, enrolledUuids, 0));
  }

  public static ExperimentEnrollmentManager withEnrollment(final String experimentName, final UUID enrolledUuid) {
    return new ExperimentEnrollmentManager(withEnrollment(experimentName, Set.of(enrolledUuid), 0));
  }
}
