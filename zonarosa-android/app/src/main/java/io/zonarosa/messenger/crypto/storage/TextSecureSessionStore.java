package io.zonarosa.messenger.crypto.storage;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.zonarosa.core.util.logging.Log;
import io.zonarosa.libzonarosa.protocol.NoSessionException;
import io.zonarosa.libzonarosa.protocol.ZonaRosaProtocolAddress;
import io.zonarosa.libzonarosa.protocol.state.SessionRecord;
import io.zonarosa.messenger.crypto.ReentrantSessionLock;
import io.zonarosa.messenger.database.SessionTable;
import io.zonarosa.messenger.database.ZonaRosaDatabase;
import io.zonarosa.messenger.recipients.Recipient;
import io.zonarosa.messenger.recipients.RecipientId;
import io.zonarosa.service.api.ZonaRosaServiceSessionStore;
import io.zonarosa.service.api.ZonaRosaSessionLock;
import io.zonarosa.core.models.ServiceId;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ZonaRosaSessionStore implements ZonaRosaServiceSessionStore {

  private static final String TAG = Log.tag(ZonaRosaSessionStore.class);

  private final ServiceId accountId;

  public ZonaRosaSessionStore(@NonNull ServiceId accountId) {
    this.accountId = accountId;
  }

  @Override
  public SessionRecord loadSession(@NonNull ZonaRosaProtocolAddress address) {
    try (ZonaRosaSessionLock.Lock unused = ReentrantSessionLock.INSTANCE.acquire()) {
      SessionRecord sessionRecord = ZonaRosaDatabase.sessions().load(accountId, address);

      if (sessionRecord == null) {
        Log.w(TAG, "No existing session information found for " + address);
        return new SessionRecord();
      }

      return sessionRecord;
    }
  }

  @Override
  public List<SessionRecord> loadExistingSessions(List<ZonaRosaProtocolAddress> addresses) throws NoSessionException {
    try (ZonaRosaSessionLock.Lock unused = ReentrantSessionLock.INSTANCE.acquire()) {
      List<SessionRecord> sessionRecords = ZonaRosaDatabase.sessions().load(accountId, addresses);

      if (sessionRecords.size() != addresses.size()) {
        String message = "Mismatch! Asked for " + addresses.size() + " sessions, but only found " + sessionRecords.size() + "!";
        Log.w(TAG, message);
        throw new NoSessionException(message);
      }

      if (sessionRecords.stream().anyMatch(Objects::isNull)) {
        throw new NoSessionException("Failed to find one or more sessions.");
      }

      return sessionRecords;
    }
  }

  @Override
  public void storeSession(@NonNull ZonaRosaProtocolAddress address, @NonNull SessionRecord record) {
    try (ZonaRosaSessionLock.Lock unused = ReentrantSessionLock.INSTANCE.acquire()) {
      ZonaRosaDatabase.sessions().store(accountId, address, record);
    }
  }

  @Override
  public boolean containsSession(ZonaRosaProtocolAddress address) {
    try (ZonaRosaSessionLock.Lock unused = ReentrantSessionLock.INSTANCE.acquire()) {
      SessionRecord sessionRecord = ZonaRosaDatabase.sessions().load(accountId, address);

      return sessionRecord != null && sessionRecord.hasSenderChain();
    }
  }

  @Override
  public void deleteSession(ZonaRosaProtocolAddress address) {
    try (ZonaRosaSessionLock.Lock unused = ReentrantSessionLock.INSTANCE.acquire()) {
      Log.w(TAG, "Deleting session for " + address);
      ZonaRosaDatabase.sessions().delete(accountId, address);
    }
  }

  @Override
  public void deleteAllSessions(String name) {
    try (ZonaRosaSessionLock.Lock unused = ReentrantSessionLock.INSTANCE.acquire()) {
      Log.w(TAG, "Deleting all sessions for " + name);
      ZonaRosaDatabase.sessions().deleteAllFor(accountId, name);
    }
  }

  @Override
  public List<Integer> getSubDeviceSessions(String name) {
    try (ZonaRosaSessionLock.Lock unused = ReentrantSessionLock.INSTANCE.acquire()) {
      return ZonaRosaDatabase.sessions().getSubDevices(accountId, name);
    }
  }

  @Override
  public Map<ZonaRosaProtocolAddress, SessionRecord> getAllAddressesWithActiveSessions(List<String> addressNames) {
    try (ZonaRosaSessionLock.Lock unused = ReentrantSessionLock.INSTANCE.acquire()) {
      return ZonaRosaDatabase.sessions()
                           .getAllFor(accountId, addressNames)
                           .stream()
                           .filter(row -> isActive(row.getRecord()))
                           .collect(Collectors.toMap(row -> new ZonaRosaProtocolAddress(row.getAddress(), row.getDeviceId()), SessionTable.SessionRow::getRecord));
    }
  }

  @Override
  public void archiveSession(ZonaRosaProtocolAddress address) {
    try (ZonaRosaSessionLock.Lock unused = ReentrantSessionLock.INSTANCE.acquire()) {
      SessionRecord session = ZonaRosaDatabase.sessions().load(accountId, address);
      if (session != null) {
        session.archiveCurrentState();
        ZonaRosaDatabase.sessions().store(accountId, address, session);
      }
    }
  }
  
  public void archiveSession(@NonNull ServiceId serviceId, int deviceId) {
    try (ZonaRosaSessionLock.Lock unused = ReentrantSessionLock.INSTANCE.acquire()) {
      archiveSession(new ZonaRosaProtocolAddress(serviceId.toString(), deviceId));
    }
  }

  public void archiveSessions(@NonNull RecipientId recipientId, int deviceId) {
    try (ZonaRosaSessionLock.Lock unused = ReentrantSessionLock.INSTANCE.acquire()) {
      Recipient recipient = Recipient.resolved(recipientId);

      if (recipient.getHasAci()) {
        archiveSession(new ZonaRosaProtocolAddress(recipient.requireAci().toString(), deviceId));
      }

      if (recipient.getHasPni()) {
        archiveSession(new ZonaRosaProtocolAddress(recipient.requirePni().toString(), deviceId));
      }

      if (recipient.getHasE164()) {
        archiveSession(new ZonaRosaProtocolAddress(recipient.requireE164(), deviceId));
      }
    }
  }

  public void archiveSessions(@NonNull RecipientId recipientId) {
    try (ZonaRosaSessionLock.Lock unused = ReentrantSessionLock.INSTANCE.acquire()) {
      Recipient recipient = Recipient.resolved(recipientId);

      if (recipient.getHasAci()) {
        ZonaRosaProtocolAddress address = new ZonaRosaProtocolAddress(recipient.requireAci().toString(), 1);
        archiveSiblingSessions(address);
        archiveSession(address);
      }

      if (recipient.getHasPni()) {
        ZonaRosaProtocolAddress address = new ZonaRosaProtocolAddress(recipient.requirePni().toString(), 1);
        archiveSiblingSessions(address);
        archiveSession(address);
      }

      if (recipient.getHasE164()) {
        ZonaRosaProtocolAddress address = new ZonaRosaProtocolAddress(recipient.requireE164(), 1);
        archiveSiblingSessions(address);
        archiveSession(address);
      }
    }
  }

  public void archiveSiblingSessions(@NonNull ZonaRosaProtocolAddress address) {
    try (ZonaRosaSessionLock.Lock unused = ReentrantSessionLock.INSTANCE.acquire()) {
      List<SessionTable.SessionRow> sessions = ZonaRosaDatabase.sessions().getAllFor(accountId, address.getName());

      for (SessionTable.SessionRow row : sessions) {
        if (row.getDeviceId() != address.getDeviceId()) {
          row.getRecord().archiveCurrentState();
          storeSession(new ZonaRosaProtocolAddress(row.getAddress(), row.getDeviceId()), row.getRecord());
        }
      }
    }
  }

  public void archiveAllSessions() {
    try (ZonaRosaSessionLock.Lock unused = ReentrantSessionLock.INSTANCE.acquire()) {
      List<SessionTable.SessionRow> sessions = ZonaRosaDatabase.sessions().getAll(accountId);

      for (SessionTable.SessionRow row : sessions) {
        row.getRecord().archiveCurrentState();
        storeSession(new ZonaRosaProtocolAddress(row.getAddress(), row.getDeviceId()), row.getRecord());
      }
    }
  }

  private static boolean isActive(@Nullable SessionRecord record) {
    return record != null && record.hasSenderChain();
  }
}
