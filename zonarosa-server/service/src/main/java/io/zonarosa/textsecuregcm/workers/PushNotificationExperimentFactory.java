package io.zonarosa.server.workers;

import io.zonarosa.server.WhisperServerConfiguration;
import io.zonarosa.server.experiment.PushNotificationExperiment;

public interface PushNotificationExperimentFactory<T> {

  PushNotificationExperiment<T> buildExperiment(CommandDependencies commandDependencies,
      WhisperServerConfiguration configuration);
}
