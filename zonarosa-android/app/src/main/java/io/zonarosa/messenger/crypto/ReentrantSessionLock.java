package io.zonarosa.messenger.crypto;

import io.zonarosa.service.api.ZonaRosaSessionLock;

import java.util.concurrent.locks.ReentrantLock;

/**
 * An implementation of {@link ZonaRosaSessionLock} that is backed by a {@link ReentrantLock}.
 */
public enum ReentrantSessionLock implements ZonaRosaSessionLock {

  INSTANCE;

  private static final ReentrantLock LOCK = new ReentrantLock();

  @Override
  public Lock acquire() {
    LOCK.lock();
    return LOCK::unlock;
  }

  public boolean isHeldByCurrentThread() {
    return LOCK.isHeldByCurrentThread();
  }
}
