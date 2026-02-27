/*
 * Copyright 2013-2021 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.workers;

import com.fasterxml.jackson.databind.DeserializationFeature;
import io.dropwizard.core.Application;
import io.dropwizard.core.cli.EnvironmentCommand;
import io.dropwizard.core.setup.Environment;

import java.util.List;
import java.util.UUID;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import io.zonarosa.server.WhisperServerConfiguration;
import io.zonarosa.server.storage.Account;
import io.zonarosa.server.storage.Device;

public class UnlinkDeviceCommand extends AbstractCommandWithDependencies {

  public UnlinkDeviceCommand() {
    super(new Application<>() {
      @Override
      public void run(WhisperServerConfiguration configuration, Environment environment) {

      }
    }, "unlink-device", "Unlink a device and clear messages");
  }

  @Override
  public void configure(final Subparser subparser) {
    super.configure(subparser);

    subparser.addArgument("-d", "--deviceId")
        .dest("deviceIds")
        .type(Byte.class)
        .action(Arguments.append())
        .required(true);

    subparser.addArgument("-u", "--uuid")
        .help("the UUID of the account to modify")
        .dest("uuid")
        .type(String.class)
        .required(true);
  }

  @Override
  protected void run(final Environment environment, final Namespace namespace,
      final WhisperServerConfiguration configuration,
      final CommandDependencies deps) throws Exception {
    final UUID aci = UUID.fromString(namespace.getString("uuid").trim());
    final List<Byte> deviceIds = namespace.getList("deviceIds");

    Account account = deps.accountsManager().getByAccountIdentifier(aci)
        .orElseThrow(() -> new IllegalArgumentException("account id " + aci + " does not exist"));

    if (deviceIds.contains(Device.PRIMARY_ID)) {
      throw new IllegalArgumentException("cannot delete primary device");
    }

    for (byte deviceId : deviceIds) {
      /** see {@link io.zonarosa.server.controllers.DeviceController#removeDevice} */
      System.out.format("Removing device %s::%d\n", aci, deviceId);
      deps.accountsManager().removeDevice(account, deviceId).join();
    }
  }
}
