/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.workers;

import io.dropwizard.core.Application;
import io.dropwizard.core.cli.ServerCommand;
import io.dropwizard.core.server.DefaultServerFactory;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.jetty.HttpsConnectorFactory;
import java.time.Duration;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import io.zonarosa.server.WhisperServerConfiguration;
import io.zonarosa.server.metrics.MetricsUtil;
import io.zonarosa.server.storage.MessagePersister;
import io.zonarosa.server.util.logging.UncaughtExceptionHandler;

public class MessagePersisterServiceCommand extends ServerCommand<WhisperServerConfiguration> {

  private static final String WORKER_COUNT = "workers";

  public MessagePersisterServiceCommand() {
    super(new Application<>() {
            @Override
            public void run(WhisperServerConfiguration configuration, Environment environment) {

            }
          }, "message-persister-service",
        "Starts a persistent service to persist undelivered messages from Redis to Dynamo DB");
  }

  @Override
  public void configure(final Subparser subparser) {
    super.configure(subparser);
    subparser.addArgument("--workers")
        .type(Integer.class)
        .dest(WORKER_COUNT)
        .required(true)
        .help("The number of worker threads");
  }

  @Override
  protected void run(Environment environment, Namespace namespace, WhisperServerConfiguration configuration)
      throws Exception {

    UncaughtExceptionHandler.register();

    final CommandDependencies deps = CommandDependencies.build("message-persister-service", environment, configuration);
    MetricsUtil.configureRegistries(configuration, environment, deps.dynamicConfigurationManager());

    if (configuration.getServerFactory() instanceof DefaultServerFactory defaultServerFactory) {
      defaultServerFactory.getApplicationConnectors()
          .forEach(connectorFactory -> {
            if (connectorFactory instanceof HttpsConnectorFactory h) {
              h.setKeyStorePassword(configuration.getTlsKeyStoreConfiguration().password().value());
            }
          });
    }

    final MessagePersister messagePersister = new MessagePersister(deps.messagesCache(),
        deps.messagesManager(),
        deps.accountsManager(),
        deps.dynamicConfigurationManager(),
        Duration.ofMinutes(configuration.getMessageCacheConfiguration().getPersistDelayMinutes()),
        namespace.getInt(WORKER_COUNT));

    environment.lifecycle().manage(messagePersister);

    super.run(environment, namespace, configuration);
  }

}
