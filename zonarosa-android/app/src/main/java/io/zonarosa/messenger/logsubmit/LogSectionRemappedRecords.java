package io.zonarosa.messenger.logsubmit;

import android.content.Context;
import android.database.Cursor;

import androidx.annotation.NonNull;

import io.zonarosa.core.util.AsciiArt;
import io.zonarosa.messenger.database.ZonaRosaDatabase;

/**
 * Renders data pertaining to sender key. While all private info is obfuscated, this is still only intended to be printed for internal users.
 */
public class LogSectionRemappedRecords implements LogSection {

  @Override
  public @NonNull String getTitle() {
    return "REMAPPED RECORDS";
  }

  @Override
  public @NonNull CharSequence getContent(@NonNull Context context) {
    StringBuilder builder = new StringBuilder();

    builder.append("--- Recipients").append("\n\n");
    try (Cursor cursor = ZonaRosaDatabase.remappedRecords().getAllRecipients()) {
      builder.append(AsciiArt.tableFor(cursor)).append("\n\n");
    }

    builder.append("--- Threads").append("\n\n");
    try (Cursor cursor = ZonaRosaDatabase.remappedRecords().getAllThreads()) {
      builder.append(AsciiArt.tableFor(cursor)).append("\n");
    }

    return builder;
  }
}
