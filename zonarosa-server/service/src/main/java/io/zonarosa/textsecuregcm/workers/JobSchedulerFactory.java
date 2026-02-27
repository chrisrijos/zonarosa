package io.zonarosa.server.workers;

import io.zonarosa.server.WhisperServerConfiguration;
import io.zonarosa.server.scheduler.JobScheduler;

public interface JobSchedulerFactory {

  JobScheduler buildJobScheduler(CommandDependencies commandDependencies, WhisperServerConfiguration configuration);
}
