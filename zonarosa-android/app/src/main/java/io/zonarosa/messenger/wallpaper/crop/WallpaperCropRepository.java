package io.zonarosa.messenger.wallpaper.crop;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import io.zonarosa.core.util.logging.Log;
import io.zonarosa.messenger.database.ZonaRosaDatabase;
import io.zonarosa.messenger.dependencies.AppDependencies;
import io.zonarosa.messenger.keyvalue.ZonaRosaStore;
import io.zonarosa.messenger.recipients.RecipientId;
import io.zonarosa.messenger.wallpaper.ChatWallpaper;
import io.zonarosa.messenger.wallpaper.WallpaperStorage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

final class WallpaperCropRepository {

  private static final String TAG = Log.tag(WallpaperCropRepository.class);

  @Nullable private final RecipientId recipientId;
  private final           Context     context;

  public WallpaperCropRepository(@Nullable RecipientId recipientId) {
    this.context     = AppDependencies.getApplication();
    this.recipientId = recipientId;
  }

  @WorkerThread
  @NonNull ChatWallpaper setWallPaper(byte[] bytes) throws IOException {
    try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
      ChatWallpaper wallpaper = WallpaperStorage.save(inputStream);

      if (recipientId != null) {
        Log.i(TAG, "Setting image wallpaper for " + recipientId);
        ZonaRosaDatabase.recipients().setWallpaper(recipientId, wallpaper, true);
      } else {
        Log.i(TAG, "Setting image wallpaper for default");
        ZonaRosaStore.wallpaper().setWallpaper(wallpaper);
      }

      return wallpaper;
    }
  }
}
