//
// Copyright 2023 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.zkgroup.backups;

import static io.zonarosa.libzonarosa.internal.FilterExceptions.filterExceptions;

import java.time.Instant;
import io.zonarosa.libzonarosa.internal.Native;
import io.zonarosa.libzonarosa.zkgroup.GenericServerSecretParams;
import io.zonarosa.libzonarosa.zkgroup.InvalidInputException;
import io.zonarosa.libzonarosa.zkgroup.VerificationFailedException;
import io.zonarosa.libzonarosa.zkgroup.internal.ByteArray;

public final class BackupAuthCredentialPresentation extends ByteArray {

  public BackupAuthCredentialPresentation(byte[] contents) throws InvalidInputException {
    super(contents);
    filterExceptions(
        InvalidInputException.class,
        () -> Native.BackupAuthCredentialPresentation_CheckValidContents(contents));
  }

  public void verify(GenericServerSecretParams serverParams) throws VerificationFailedException {
    verify(Instant.now(), serverParams);
  }

  public void verify(Instant currentTime, GenericServerSecretParams serverParams)
      throws VerificationFailedException {
    filterExceptions(
        VerificationFailedException.class,
        () ->
            Native.BackupAuthCredentialPresentation_Verify(
                getInternalContentsForJNI(),
                currentTime.getEpochSecond(),
                serverParams.getInternalContentsForJNI()));
  }

  public byte[] getBackupId() {
    return Native.BackupAuthCredentialPresentation_GetBackupId(getInternalContentsForJNI());
  }

  public BackupLevel getBackupLevel() {
    return BackupLevel.fromValue(
        Native.BackupAuthCredentialPresentation_GetBackupLevel(getInternalContentsForJNI()));
  }

  public BackupCredentialType getType() {
    return BackupCredentialType.fromValue(
        Native.BackupAuthCredentialPresentation_GetType(getInternalContentsForJNI()));
  }
}
