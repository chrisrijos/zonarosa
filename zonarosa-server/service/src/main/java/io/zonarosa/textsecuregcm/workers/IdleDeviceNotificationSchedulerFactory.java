package io.zonarosa.server.workers;

import io.zonarosa.server.WhisperServerConfiguration;
import io.zonarosa.server.push.IdleDeviceNotificationScheduler;
import io.zonarosa.server.scheduler.JobScheduler;
import java.time.Clock;

public class IdleDeviceNotificationSchedulerFactory implements JobSchedulerFactory {

  @Override
  public JobScheduler buildJobScheduler(final CommandDependencies commandDependencies,
      final WhisperServerConfiguration configuration) {

    return new IdleDeviceNotificationScheduler(commandDependencies.accountsManager(),
        commandDependencies.pushNotificationManager(),
        commandDependencies.dynamoDbAsyncClient(),
        configuration.getDynamoDbTables().getScheduledJobs().getTableName(),
        configuration.getDynamoDbTables().getScheduledJobs().getExpiration(),
        Clock.systemUTC());
  }
}
