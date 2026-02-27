package io.zonarosa.messenger.jobmanager.impl;

import androidx.annotation.NonNull;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import io.zonarosa.core.util.logging.Log;
import io.zonarosa.messenger.jobmanager.ConstraintObserver;

public class SqlCipherMigrationConstraintObserver implements ConstraintObserver {

  private static final String REASON = Log.tag(SqlCipherMigrationConstraintObserver.class);

  private Notifier notifier;

  public SqlCipherMigrationConstraintObserver() {
    EventBus.getDefault().register(this);
  }

  @Override
  public void register(@NonNull Notifier notifier) {
    this.notifier = notifier;
  }

  @Subscribe(threadMode = ThreadMode.MAIN)
  public void onEvent(SqlCipherNeedsMigrationEvent event) {
    if (notifier != null) notifier.onConstraintMet(REASON);
  }

  public static class SqlCipherNeedsMigrationEvent {
  }
}
