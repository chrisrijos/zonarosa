package io.zonarosa.messenger.crypto.storage;

import android.content.Context;

import androidx.annotation.NonNull;

import io.zonarosa.libzonarosa.protocol.IdentityKey;
import io.zonarosa.libzonarosa.protocol.IdentityKeyPair;
import io.zonarosa.libzonarosa.protocol.InvalidKeyIdException;
import io.zonarosa.libzonarosa.protocol.NoSessionException;
import io.zonarosa.libzonarosa.protocol.ZonaRosaProtocolAddress;
import io.zonarosa.libzonarosa.protocol.ecc.ECPublicKey;
import io.zonarosa.libzonarosa.protocol.groups.state.SenderKeyRecord;
import io.zonarosa.libzonarosa.protocol.state.KyberPreKeyRecord;
import io.zonarosa.libzonarosa.protocol.state.PreKeyRecord;
import io.zonarosa.libzonarosa.protocol.state.SessionRecord;
import io.zonarosa.libzonarosa.protocol.state.SignedPreKeyRecord;
import io.zonarosa.messenger.keyvalue.ZonaRosaStore;
import io.zonarosa.messenger.util.ZonaRosaPreferences;
import io.zonarosa.service.api.ZonaRosaServiceAccountDataStore;
import io.zonarosa.service.api.push.DistributionId;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ZonaRosaServiceAccountDataStoreImpl implements ZonaRosaServiceAccountDataStore {

  private final Context                context;
  private final ZonaRosaPreKeyStore  preKeyStore;
  private final ZonaRosaPreKeyStore  signedPreKeyStore;
  private final ZonaRosaIdentityKeyStore identityKeyStore;
  private final ZonaRosaSessionStore sessionStore;
  private final ZonaRosaSenderKeyStore   senderKeyStore;
  private final ZonaRosaKyberPreKeyStore kyberPreKeyStore;

  public ZonaRosaServiceAccountDataStoreImpl(@NonNull Context context,
                                           @NonNull ZonaRosaPreKeyStore preKeyStore,
                                           @NonNull ZonaRosaKyberPreKeyStore kyberPreKeyStore,
                                           @NonNull ZonaRosaIdentityKeyStore identityKeyStore,
                                           @NonNull ZonaRosaSessionStore sessionStore,
                                           @NonNull ZonaRosaSenderKeyStore senderKeyStore)
  {
    this.context           = context;
    this.preKeyStore       = preKeyStore;
    this.kyberPreKeyStore  = kyberPreKeyStore;
    this.signedPreKeyStore = preKeyStore;
    this.identityKeyStore  = identityKeyStore;
    this.sessionStore      = sessionStore;
    this.senderKeyStore    = senderKeyStore;
  }

  @Override
  public boolean isMultiDevice() {
    return ZonaRosaStore.account().isMultiDevice();
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
  public boolean isTrustedIdentity(ZonaRosaProtocolAddress address, IdentityKey identityKey, Direction direction) {
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
  public void markAllOneTimeEcPreKeysStaleIfNecessary(long staleTime) {
    preKeyStore.markAllOneTimeEcPreKeysStaleIfNecessary(staleTime);
  }

  @Override
  public void deleteAllStaleOneTimeEcPreKeys(long threshold, int minCount) {
    preKeyStore.deleteAllStaleOneTimeEcPreKeys(threshold, minCount);
  }

  @Override
  public SessionRecord loadSession(ZonaRosaProtocolAddress axolotlAddress) {
    return sessionStore.loadSession(axolotlAddress);
  }

  @Override
  public List<SessionRecord> loadExistingSessions(List<ZonaRosaProtocolAddress> addresses) throws NoSessionException {
    return sessionStore.loadExistingSessions(addresses);
  }

  @Override
  public List<Integer> getSubDeviceSessions(String number) {
    return sessionStore.getSubDeviceSessions(number);
  }

  @Override
  public Map<ZonaRosaProtocolAddress, SessionRecord> getAllAddressesWithActiveSessions(List<String> addressNames) {
    return sessionStore.getAllAddressesWithActiveSessions(addressNames);
  }

  @Override
  public void storeSession(ZonaRosaProtocolAddress axolotlAddress, SessionRecord record) {
    sessionStore.storeSession(axolotlAddress, record);
  }

  @Override
  public boolean containsSession(ZonaRosaProtocolAddress axolotlAddress) {
    return sessionStore.containsSession(axolotlAddress);
  }

  @Override
  public void deleteSession(ZonaRosaProtocolAddress axolotlAddress) {
    sessionStore.deleteSession(axolotlAddress);
  }

  @Override
  public void deleteAllSessions(String number) {
    sessionStore.deleteAllSessions(number);
  }

  @Override
  public void archiveSession(ZonaRosaProtocolAddress address) {
    sessionStore.archiveSession(address);
    senderKeyStore.clearSenderKeySharedWith(Collections.singleton(address));
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
  public KyberPreKeyRecord loadKyberPreKey(int kyberPreKeyId) throws InvalidKeyIdException {
    return kyberPreKeyStore.loadKyberPreKey(kyberPreKeyId);
  }

  @Override
  public List<KyberPreKeyRecord> loadKyberPreKeys() {
    return kyberPreKeyStore.loadKyberPreKeys();
  }

  @Override
  public @NonNull List<KyberPreKeyRecord> loadLastResortKyberPreKeys() {
    return kyberPreKeyStore.loadLastResortKyberPreKeys();
  }

  @Override
  public void storeKyberPreKey(int kyberPreKeyId, KyberPreKeyRecord record) {
    kyberPreKeyStore.storeKyberPreKey(kyberPreKeyId, record);
  }

  @Override
  public void storeLastResortKyberPreKey(int kyberPreKeyId, @NonNull KyberPreKeyRecord kyberPreKeyRecord) {
    kyberPreKeyStore.storeLastResortKyberPreKey(kyberPreKeyId, kyberPreKeyRecord);
  }

  @Override
  public boolean containsKyberPreKey(int kyberPreKeyId) {
    return kyberPreKeyStore.containsKyberPreKey(kyberPreKeyId);
  }

  @Override
  public void markKyberPreKeyUsed(int kyberPreKeyId, int signedKeyId, ECPublicKey publicKey) {
    kyberPreKeyStore.markKyberPreKeyUsed(kyberPreKeyId, signedKeyId, publicKey);
  }

  @Override
  public void removeKyberPreKey(int kyberPreKeyId) {
    kyberPreKeyStore.removeKyberPreKey(kyberPreKeyId);
  }

  @Override
  public void markAllOneTimeKyberPreKeysStaleIfNecessary(long staleTime) {
    kyberPreKeyStore.markAllOneTimeKyberPreKeysStaleIfNecessary(staleTime);
  }

  @Override
  public void deleteAllStaleOneTimeKyberPreKeys(long threshold, int minCount) {
    kyberPreKeyStore.deleteAllStaleOneTimeKyberPreKeys(threshold, minCount);
  }

  @Override
  public void storeSenderKey(ZonaRosaProtocolAddress sender, UUID distributionId, SenderKeyRecord record) {
    senderKeyStore.storeSenderKey(sender, distributionId, record);
  }

  @Override
  public SenderKeyRecord loadSenderKey(ZonaRosaProtocolAddress sender, UUID distributionId) {
    return senderKeyStore.loadSenderKey(sender, distributionId);
  }

  @Override
  public Set<ZonaRosaProtocolAddress> getSenderKeySharedWith(DistributionId distributionId) {
    return senderKeyStore.getSenderKeySharedWith(distributionId);
  }

  @Override
  public void markSenderKeySharedWith(DistributionId distributionId, Collection<ZonaRosaProtocolAddress> addresses) {
    senderKeyStore.markSenderKeySharedWith(distributionId, addresses);
  }

  @Override
  public void clearSenderKeySharedWith(Collection<ZonaRosaProtocolAddress> addresses) {
    senderKeyStore.clearSenderKeySharedWith(addresses);
  }

  public @NonNull ZonaRosaIdentityKeyStore identities() {
    return identityKeyStore;
  }

  public @NonNull ZonaRosaPreKeyStore preKeys() {
    return preKeyStore;
  }

  public @NonNull ZonaRosaSessionStore sessions() {
    return sessionStore;
  }

  public @NonNull ZonaRosaSenderKeyStore senderKeys() {
    return senderKeyStore;
  }
}
