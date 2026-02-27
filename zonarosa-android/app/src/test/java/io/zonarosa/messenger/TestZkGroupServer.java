package io.zonarosa.messenger;

import io.zonarosa.libzonarosa.zkgroup.ServerPublicParams;
import io.zonarosa.libzonarosa.zkgroup.ServerSecretParams;
import io.zonarosa.libzonarosa.zkgroup.VerificationFailedException;
import io.zonarosa.libzonarosa.zkgroup.groups.GroupPublicParams;
import io.zonarosa.libzonarosa.zkgroup.profiles.ProfileKeyCommitment;
import io.zonarosa.libzonarosa.zkgroup.profiles.ProfileKeyCredentialPresentation;
import io.zonarosa.libzonarosa.zkgroup.profiles.ProfileKeyCredentialRequest;
import io.zonarosa.libzonarosa.zkgroup.profiles.ServerZkProfileOperations;
import io.zonarosa.service.test.LibZonaRosaLibraryUtil;

import java.util.UUID;

/**
 * Provides Zk group operations that the server would provide.
 * Copied in app from libzonarosa
 */
public final class TestZkGroupServer {

  private final ServerPublicParams        serverPublicParams;
  private final ServerZkProfileOperations serverZkProfileOperations;

  public TestZkGroupServer() {
    LibZonaRosaLibraryUtil.assumeLibZonaRosaSupportedOnOS();

    ServerSecretParams serverSecretParams = ServerSecretParams.generate();

    serverPublicParams        = serverSecretParams.getPublicParams();
    serverZkProfileOperations = new ServerZkProfileOperations(serverSecretParams);
  }

  public ServerPublicParams getServerPublicParams() {
    return serverPublicParams;
  }
}
