package io.zonarosa.service.api.groupsv2;

import io.zonarosa.libzonarosa.zkgroup.ServerPublicParams;
import io.zonarosa.libzonarosa.zkgroup.ServerSecretParams;
import io.zonarosa.libzonarosa.zkgroup.VerificationFailedException;
import io.zonarosa.libzonarosa.zkgroup.groups.GroupPublicParams;
import io.zonarosa.libzonarosa.zkgroup.profiles.ExpiringProfileKeyCredentialResponse;
import io.zonarosa.libzonarosa.zkgroup.profiles.ProfileKeyCommitment;
import io.zonarosa.libzonarosa.zkgroup.profiles.ProfileKeyCredentialPresentation;
import io.zonarosa.libzonarosa.zkgroup.profiles.ProfileKeyCredentialRequest;
import io.zonarosa.libzonarosa.zkgroup.profiles.ServerZkProfileOperations;
import io.zonarosa.core.models.ServiceId.ACI;
import io.zonarosa.service.testutil.LibZonaRosaLibraryUtil;

import java.time.Instant;

/**
 * Provides Zk group operations that the server would provide.
 */
final class TestZkGroupServer {

  private final ServerPublicParams        serverPublicParams;
  private final ServerZkProfileOperations serverZkProfileOperations;

  TestZkGroupServer() {
    LibZonaRosaLibraryUtil.assumeLibZonaRosaSupportedOnOS();

    ServerSecretParams serverSecretParams = ServerSecretParams.generate();

    serverPublicParams        = serverSecretParams.getPublicParams();
    serverZkProfileOperations = new ServerZkProfileOperations(serverSecretParams);
  }

  public ServerPublicParams getServerPublicParams() {
    return serverPublicParams;
  }

  public ExpiringProfileKeyCredentialResponse getExpiringProfileKeyCredentialResponse(ProfileKeyCredentialRequest request, ACI aci, ProfileKeyCommitment commitment, Instant expiration) throws VerificationFailedException {
    return serverZkProfileOperations.issueExpiringProfileKeyCredential(request, aci.getLibZonaRosaAci(), commitment, expiration);
  }

  public void assertProfileKeyCredentialPresentation(GroupPublicParams publicParams, ProfileKeyCredentialPresentation profileKeyCredentialPresentation, Instant now) {
    try {
      serverZkProfileOperations.verifyProfileKeyCredentialPresentation(publicParams, profileKeyCredentialPresentation, now);
    } catch (VerificationFailedException e) {
      throw new AssertionError(e);
    }
  }
}
