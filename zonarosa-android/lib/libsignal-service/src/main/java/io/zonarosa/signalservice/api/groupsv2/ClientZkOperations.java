package io.zonarosa.service.api.groupsv2;

import io.zonarosa.libzonarosa.zkgroup.InvalidInputException;
import io.zonarosa.libzonarosa.zkgroup.ServerPublicParams;
import io.zonarosa.libzonarosa.zkgroup.auth.ClientZkAuthOperations;
import io.zonarosa.libzonarosa.zkgroup.profiles.ClientZkProfileOperations;
import io.zonarosa.libzonarosa.zkgroup.receipts.ClientZkReceiptOperations;
import io.zonarosa.service.internal.configuration.ZonaRosaServiceConfiguration;

/**
 * Contains access to all ZK group operations for the client.
 * <p>
 * Authorization and profile operations.
 */
public final class ClientZkOperations {

  private final ClientZkAuthOperations    clientZkAuthOperations;
  private final ClientZkProfileOperations clientZkProfileOperations;
  private final ClientZkReceiptOperations clientZkReceiptOperations;
  private final ServerPublicParams        serverPublicParams;

  public ClientZkOperations(ServerPublicParams serverPublicParams) {
    this.serverPublicParams        = serverPublicParams;
    this.clientZkAuthOperations    = new ClientZkAuthOperations   (serverPublicParams);
    this.clientZkProfileOperations = new ClientZkProfileOperations(serverPublicParams);
    this.clientZkReceiptOperations = new ClientZkReceiptOperations(serverPublicParams);
  }

  public static ClientZkOperations create(ZonaRosaServiceConfiguration configuration) {
    try {
      return new ClientZkOperations(new ServerPublicParams(configuration.getZkGroupServerPublicParams()));
    } catch (InvalidInputException e) {
      throw new AssertionError(e);
    }
  }

  public ClientZkAuthOperations getAuthOperations() {
    return clientZkAuthOperations;
  }

  public ClientZkProfileOperations getProfileOperations() {
    return clientZkProfileOperations;
  }

  public ClientZkReceiptOperations getReceiptOperations() {
    return clientZkReceiptOperations;
  }

  public ServerPublicParams getServerPublicParams() {
    return serverPublicParams;
  }
}
