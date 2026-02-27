//
// Copyright 2020-2021 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.zkgroup;

import static io.zonarosa.libzonarosa.internal.FilterExceptions.filterExceptions;

import java.util.Arrays;
import io.zonarosa.libzonarosa.internal.Native;
import io.zonarosa.libzonarosa.internal.NativeHandleGuard;
import io.zonarosa.libzonarosa.zkgroup.internal.ByteArray;

public final class ServerPublicParams extends NativeHandleGuard.SimpleOwner {
  public ServerPublicParams(byte[] contents) throws InvalidInputException {
    super(filterExceptions(() -> Native.ServerPublicParams_Deserialize(contents)));
  }

  ServerPublicParams(long nativeHandle) {
    super(nativeHandle);
  }

  @Override
  protected void release(long handle) {
    Native.ServerPublicParams_Destroy(handle);
  }

  /**
   * Get the serialized form of the params' endorsement key.
   *
   * <p>Allows decoupling RingRTC's use of endorsements from libzonarosa's.
   */
  public byte[] getEndorsementPublicKey() {
    return guardedMap(Native::ServerPublicParams_GetEndorsementPublicKey);
  }

  public void verifySignature(byte[] message, NotarySignature notarySignature)
      throws VerificationFailedException {
    filterExceptions(
        VerificationFailedException.class,
        () ->
            this.guardedRunChecked(
                (serverPublicParams) ->
                    Native.ServerPublicParams_VerifySignature(
                        serverPublicParams, message, notarySignature.getInternalContentsForJNI())));
  }

  public byte[] serialize() {
    return guardedMap(Native::ServerPublicParams_Serialize);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode() * 31 + Arrays.hashCode(serialize());
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;

    ServerPublicParams other = (ServerPublicParams) o;
    return ByteArray.constantTimeEqual(this.serialize(), other.serialize());
  }
}
