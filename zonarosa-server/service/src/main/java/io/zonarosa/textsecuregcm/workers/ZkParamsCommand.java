/*
 * Copyright 2013-2020 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.workers;

import io.dropwizard.core.cli.Command;
import io.dropwizard.core.setup.Bootstrap;
import java.util.Base64;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import io.zonarosa.libzonarosa.zkgroup.ServerPublicParams;
import io.zonarosa.libzonarosa.zkgroup.ServerSecretParams;

public class ZkParamsCommand extends Command {

  public ZkParamsCommand() {
    super("zkparams", "Generates server zkparams");
  }

  @Override
  public void configure(Subparser subparser) {

  }

  @Override
  public void run(Bootstrap<?> bootstrap, Namespace namespace) throws Exception {
    ServerSecretParams serverSecretParams = ServerSecretParams.generate();
    ServerPublicParams serverPublicParams = serverSecretParams.getPublicParams();

    System.out.println("Public: " + Base64.getEncoder().withoutPadding().encodeToString(serverPublicParams.serialize()));
    System.out.println("Private: " + Base64.getEncoder().withoutPadding().encodeToString(serverSecretParams.serialize()));
  }

}
