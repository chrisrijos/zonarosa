//
// Copyright 2024 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import androidx.benchmark.BenchmarkState;
import androidx.benchmark.junit4.BenchmarkRule;
import java.time.Instant;
import java.util.UUID;
import org.junit.Rule;
import org.junit.Test;
import io.zonarosa.libzonarosa.protocol.ServiceId;
import io.zonarosa.libzonarosa.zkgroup.ServerPublicParams;
import io.zonarosa.libzonarosa.zkgroup.ServerSecretParams;
import io.zonarosa.libzonarosa.zkgroup.VerificationFailedException;
import io.zonarosa.libzonarosa.zkgroup.auth.AuthCredentialWithPni;
import io.zonarosa.libzonarosa.zkgroup.auth.AuthCredentialWithPniResponse;
import io.zonarosa.libzonarosa.zkgroup.auth.ServerZkAuthOperations;
import io.zonarosa.libzonarosa.zkgroup.groups.GroupSecretParams;

public class ClientZkOperations {
  @Rule public final BenchmarkRule benchmarkRule = new BenchmarkRule();

  private final Instant now = Instant.now();

  private final ServerSecretParams serverParams = ServerSecretParams.generate();
  private final ServerPublicParams serverPublicParams = serverParams.getPublicParams();
  private final GroupSecretParams groupParams = GroupSecretParams.generate();
  private final ServerZkAuthOperations serverZkAuthOperations =
      new ServerZkAuthOperations(serverParams);
  private final io.zonarosa.libzonarosa.zkgroup.auth.ClientZkAuthOperations clientZkOperations =
      new io.zonarosa.libzonarosa.zkgroup.auth.ClientZkAuthOperations(serverPublicParams);

  private final ServiceId.Aci aci = new ServiceId.Aci(UUID.randomUUID());
  private final ServiceId.Pni pni = new ServiceId.Pni(UUID.randomUUID());

  @Test
  public void receiveAuthCredentialWithPni() throws VerificationFailedException {
    final BenchmarkState state = benchmarkRule.getState();
    state.pauseTiming();
    final AuthCredentialWithPniResponse authCredentialWithPniResponse =
        serverZkAuthOperations.issueAuthCredentialWithPniZkc(aci, pni, now);
    state.resumeTiming();

    while (state.keepRunning()) {
      clientZkOperations.receiveAuthCredentialWithPniAsServiceId(
          aci, pni, now.getEpochSecond(), authCredentialWithPniResponse);
    }
  }

  @Test
  public void createAuthCredentialPresentation() throws VerificationFailedException {
    final BenchmarkState state = benchmarkRule.getState();
    state.pauseTiming();
    final AuthCredentialWithPniResponse authCredentialWithPniResponse =
        serverZkAuthOperations.issueAuthCredentialWithPniZkc(aci, pni, now);
    final AuthCredentialWithPni authCredentialWithPni =
        clientZkOperations.receiveAuthCredentialWithPniAsServiceId(
            aci, pni, now.getEpochSecond(), authCredentialWithPniResponse);
    state.resumeTiming();

    while (state.keepRunning()) {
      clientZkOperations.createAuthCredentialPresentation(groupParams, authCredentialWithPni);
    }
  }
}
