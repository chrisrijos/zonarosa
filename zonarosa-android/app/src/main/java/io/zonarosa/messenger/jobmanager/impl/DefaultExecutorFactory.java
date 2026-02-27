package io.zonarosa.messenger.jobmanager.impl;

import android.os.Process;

import androidx.annotation.NonNull;

import io.zonarosa.core.util.ThreadUtil;
import io.zonarosa.messenger.jobmanager.ExecutorFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DefaultExecutorFactory implements ExecutorFactory {
  @Override
  public @NonNull ExecutorService newSingleThreadExecutor(@NonNull String name) {
    return Executors.newSingleThreadExecutor(r -> new Thread(r, name) {
      @Override public void run() {
        Process.setThreadPriority(ThreadUtil.PRIORITY_BACKGROUND_THREAD);
        super.run();
      }
    });
  }
}
