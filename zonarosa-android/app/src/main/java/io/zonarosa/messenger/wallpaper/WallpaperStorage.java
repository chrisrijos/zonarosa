package io.zonarosa.messenger.wallpaper;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import io.zonarosa.core.util.logging.Log;
import io.zonarosa.messenger.attachments.AttachmentId;
import io.zonarosa.messenger.database.ZonaRosaDatabase;
import io.zonarosa.messenger.dependencies.AppDependencies;
import io.zonarosa.messenger.jobs.UploadAttachmentToArchiveJob;
import io.zonarosa.messenger.keyvalue.ZonaRosaStore;
import io.zonarosa.messenger.mms.PartAuthority;
import io.zonarosa.messenger.mms.PartUriParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Manages the storage of custom wallpaper files.
 */
public final class WallpaperStorage {

  private static final String TAG = Log.tag(WallpaperStorage.class);

  /**
   * Saves the provided input stream as a new wallpaper file.
   */
  @WorkerThread
  public static @NonNull ChatWallpaper save(@NonNull InputStream wallpaperStream) throws IOException {
    AttachmentId attachmentId = ZonaRosaDatabase.attachments().insertWallpaper(wallpaperStream);

    if (ZonaRosaStore.backup().backsUpMedia()) {
      AppDependencies.getJobManager().add(new UploadAttachmentToArchiveJob(attachmentId, true));
    }

    return ChatWallpaperFactory.create(PartAuthority.getAttachmentDataUri(attachmentId));
  }

  @WorkerThread
  public static @NonNull List<ChatWallpaper> getAll() {
    return ZonaRosaDatabase.attachments()
                         .getAllWallpapers()
                         .stream()
                         .map(PartAuthority::getAttachmentDataUri)
                         .map(ChatWallpaperFactory::create)
                         .collect(Collectors.toList());
  }

  /**
   * Called when wallpaper is deselected. This will check anywhere the wallpaper could be used, and
   * if we discover it's unused, we'll delete the file.
   */
  @WorkerThread
  public static void onWallpaperDeselected(@NonNull Uri uri) {
    if (!isWallpaperUriUsed(uri)) {
      AttachmentId attachmentId = new PartUriParser(uri).getPartId();
      ZonaRosaDatabase.attachments().deleteAttachment(attachmentId);
    }
  }

  public static boolean isWallpaperUriUsed(@NonNull Uri uri) {
    Uri globalUri = ZonaRosaStore.wallpaper().getWallpaperUri();
    if (Objects.equals(uri, globalUri)) {
      return true;
    }

    int recipientCount = ZonaRosaDatabase.recipients().getWallpaperUriUsageCount(uri);
    if (recipientCount > 0) {
      return true;
    }

    return false;
  }
}
