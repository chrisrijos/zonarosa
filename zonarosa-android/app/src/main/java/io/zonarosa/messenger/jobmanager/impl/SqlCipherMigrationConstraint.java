package io.zonarosa.messenger.jobmanager.impl;

import android.app.Application;
import android.app.job.JobInfo;

import androidx.annotation.NonNull;

import io.zonarosa.messenger.jobmanager.Constraint;
import io.zonarosa.messenger.util.ZonaRosaPreferences;

public class SqlCipherMigrationConstraint implements Constraint {

  public static final String KEY = "SqlCipherMigrationConstraint";

  private final Application application;

  private SqlCipherMigrationConstraint(@NonNull Application application) {
    this.application = application;
  }

  @Override
  public boolean isMet() {
    return !ZonaRosaPreferences.getNeedsSqlCipherMigration(application);
  }

  @NonNull
  @Override
  public String getFactoryKey() {
    return KEY;
  }

  @Override
  public void applyToJobInfo(@NonNull JobInfo.Builder jobInfoBuilder) {
  }

  public static final class Factory implements Constraint.Factory<SqlCipherMigrationConstraint> {

    private final Application application;

    public Factory(@NonNull Application application) {
      this.application = application;
    }

    @Override
    public SqlCipherMigrationConstraint create() {
      return new SqlCipherMigrationConstraint(application);
    }
  }
}
