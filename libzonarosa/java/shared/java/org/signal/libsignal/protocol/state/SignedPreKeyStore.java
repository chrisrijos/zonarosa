//
// Copyright 2014-2016 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.protocol.state;

import java.util.List;
import io.zonarosa.libzonarosa.internal.CalledFromNative;
import io.zonarosa.libzonarosa.protocol.InvalidKeyIdException;

@CalledFromNative
public interface SignedPreKeyStore {

  /**
   * Load a local SignedPreKeyRecord.
   *
   * @param signedPreKeyId the ID of the local SignedPreKeyRecord.
   * @return the corresponding SignedPreKeyRecord.
   * @throws InvalidKeyIdException when there is no corresponding SignedPreKeyRecord.
   */
  public SignedPreKeyRecord loadSignedPreKey(int signedPreKeyId) throws InvalidKeyIdException;

  /**
   * Load all local SignedPreKeyRecords.
   *
   * @return All stored SignedPreKeyRecords.
   */
  public List<SignedPreKeyRecord> loadSignedPreKeys();

  /**
   * Store a local SignedPreKeyRecord.
   *
   * @param signedPreKeyId the ID of the SignedPreKeyRecord to store.
   * @param record the SignedPreKeyRecord.
   */
  public void storeSignedPreKey(int signedPreKeyId, SignedPreKeyRecord record);

  /**
   * @param signedPreKeyId A SignedPreKeyRecord ID.
   * @return true if the store has a record for the signedPreKeyId, otherwise false.
   */
  public boolean containsSignedPreKey(int signedPreKeyId);

  /**
   * Delete a SignedPreKeyRecord from local storage.
   *
   * @param signedPreKeyId The ID of the SignedPreKeyRecord to remove.
   */
  public void removeSignedPreKey(int signedPreKeyId);
}
