//
// Copyright 2014-2016 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.protocol.state.impl;

import java.util.List;
import java.util.UUID;
import io.zonarosa.libzonarosa.protocol.IdentityKey;
import io.zonarosa.libzonarosa.protocol.IdentityKeyPair;
import io.zonarosa.libzonarosa.protocol.InvalidKeyIdException;
import io.zonarosa.libzonarosa.protocol.NoSessionException;
import io.zonarosa.libzonarosa.protocol.ReusedBaseKeyException;
import io.zonarosa.libzonarosa.protocol.ZonaRosaProtocolAddress;
import io.zonarosa.libzonarosa.protocol.ecc.ECPublicKey;
import io.zonarosa.libzonarosa.protocol.groups.state.InMemorySenderKeyStore;
import io.zonarosa.libzonarosa.protocol.groups.state.SenderKeyRecord;
import io.zonarosa.libzonarosa.protocol.state.KyberPreKeyRecord;
import io.zonarosa.libzonarosa.protocol.state.PreKeyRecord;
import io.zonarosa.libzonarosa.protocol.state.SessionRecord;
import io.zonarosa.libzonarosa.protocol.state.ZonaRosaProtocolStore;
import io.zonarosa.libzonarosa.protocol.state.SignedPreKeyRecord;

public class InMemoryZonaRosaProtocolStore implements ZonaRosaProtocolStore {

  private final InMemoryPreKeyStore preKeyStore = new InMemoryPreKeyStore();
  private final InMemorySessionStore sessionStore = new InMemorySessionStore();
  private final InMemorySignedPreKeyStore signedPreKeyStore = new InMemorySignedPreKeyStore();
  private final InMemoryKyberPreKeyStore kyberPreKeyStore = new InMemoryKyberPreKeyStore();
  private final InMemorySenderKeyStore senderKeyStore = new InMemorySenderKeyStore();

  private final InMemoryIdentityKeyStore identityKeyStore;

  public InMemoryZonaRosaProtocolStore(IdentityKeyPair identityKeyPair, int registrationId) {
    this.identityKeyStore = new InMemoryIdentityKeyStore(identityKeyPair, registrationId);
  }

  @Override
  public IdentityKeyPair getIdentityKeyPair() {
    return identityKeyStore.getIdentityKeyPair();
  }

  @Override
  public int getLocalRegistrationId() {
    return identityKeyStore.getLocalRegistrationId();
  }

  @Override
  public IdentityChange saveIdentity(ZonaRosaProtocolAddress address, IdentityKey identityKey) {
    return identityKeyStore.saveIdentity(address, identityKey);
  }

  @Override
  public boolean isTrustedIdentity(
      ZonaRosaProtocolAddress address, IdentityKey identityKey, Direction direction) {
    return identityKeyStore.isTrustedIdentity(address, identityKey, direction);
  }

  @Override
  public IdentityKey getIdentity(ZonaRosaProtocolAddress address) {
    return identityKeyStore.getIdentity(address);
  }

  @Override
  public PreKeyRecord loadPreKey(int preKeyId) throws InvalidKeyIdException {
    return preKeyStore.loadPreKey(preKeyId);
  }

  @Override
  public void storePreKey(int preKeyId, PreKeyRecord record) {
    preKeyStore.storePreKey(preKeyId, record);
  }

  @Override
  public boolean containsPreKey(int preKeyId) {
    return preKeyStore.containsPreKey(preKeyId);
  }

  @Override
  public void removePreKey(int preKeyId) {
    preKeyStore.removePreKey(preKeyId);
  }

  @Override
  public SessionRecord loadSession(ZonaRosaProtocolAddress address) {
    return sessionStore.loadSession(address);
  }

  @Override
  public List<SessionRecord> loadExistingSessions(List<ZonaRosaProtocolAddress> addresses)
      throws NoSessionException {
    return sessionStore.loadExistingSessions(addresses);
  }

  @Override
  public List<Integer> getSubDeviceSessions(String name) {
    return sessionStore.getSubDeviceSessions(name);
  }

  @Override
  public void storeSession(ZonaRosaProtocolAddress address, SessionRecord record) {
    sessionStore.storeSession(address, record);
  }

  @Override
  public boolean containsSession(ZonaRosaProtocolAddress address) {
    return sessionStore.containsSession(address);
  }

  @Override
  public void deleteSession(ZonaRosaProtocolAddress address) {
    sessionStore.deleteSession(address);
  }

  @Override
  public void deleteAllSessions(String name) {
    sessionStore.deleteAllSessions(name);
  }

  @Override
  public SignedPreKeyRecord loadSignedPreKey(int signedPreKeyId) throws InvalidKeyIdException {
    return signedPreKeyStore.loadSignedPreKey(signedPreKeyId);
  }

  @Override
  public List<SignedPreKeyRecord> loadSignedPreKeys() {
    return signedPreKeyStore.loadSignedPreKeys();
  }

  @Override
  public void storeSignedPreKey(int signedPreKeyId, SignedPreKeyRecord record) {
    signedPreKeyStore.storeSignedPreKey(signedPreKeyId, record);
  }

  @Override
  public boolean containsSignedPreKey(int signedPreKeyId) {
    return signedPreKeyStore.containsSignedPreKey(signedPreKeyId);
  }

  @Override
  public void removeSignedPreKey(int signedPreKeyId) {
    signedPreKeyStore.removeSignedPreKey(signedPreKeyId);
  }

  @Override
  public void storeSenderKey(
      ZonaRosaProtocolAddress sender, UUID distributionId, SenderKeyRecord record) {
    senderKeyStore.storeSenderKey(sender, distributionId, record);
  }

  @Override
  public SenderKeyRecord loadSenderKey(ZonaRosaProtocolAddress sender, UUID distributionId) {
    return senderKeyStore.loadSenderKey(sender, distributionId);
  }

  @Override
  public KyberPreKeyRecord loadKyberPreKey(int kyberPreKeyId) throws InvalidKeyIdException {
    return kyberPreKeyStore.loadKyberPreKey(kyberPreKeyId);
  }

  @Override
  public List<KyberPreKeyRecord> loadKyberPreKeys() {
    return kyberPreKeyStore.loadKyberPreKeys();
  }

  @Override
  public void storeKyberPreKey(int kyberPreKeyId, KyberPreKeyRecord record) {
    kyberPreKeyStore.storeKyberPreKey(kyberPreKeyId, record);
  }

  @Override
  public boolean containsKyberPreKey(int kyberPreKeyId) {
    return kyberPreKeyStore.containsKyberPreKey(kyberPreKeyId);
  }

  @Override
  public void markKyberPreKeyUsed(int kyberPreKeyId, int signedPreKeyId, ECPublicKey baseKey)
      throws ReusedBaseKeyException {
    kyberPreKeyStore.markKyberPreKeyUsed(kyberPreKeyId, signedPreKeyId, baseKey);
  }

  public boolean hasKyberPreKeyBeenUsed(int kyberPreKeyId) {
    return kyberPreKeyStore.hasKyberPreKeyBeenUsed(kyberPreKeyId);
  }
}
