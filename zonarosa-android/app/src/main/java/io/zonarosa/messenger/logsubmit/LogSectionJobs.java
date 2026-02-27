package io.zonarosa.messenger.logsubmit;

import android.content.Context;

import androidx.annotation.NonNull;

import io.zonarosa.messenger.dependencies.AppDependencies;

public class LogSectionJobs implements LogSection {

  @Override
  public @NonNull String getTitle() {
    return "JOBS";
  }

  @Override
  public @NonNull CharSequence getContent(@NonNull Context context) {
    return AppDependencies.getJobManager().getDebugInfo();
  }
}
