package io.zonarosa.messenger.devicetransfer.olddevice;

import android.content.Context;

import androidx.annotation.NonNull;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import io.zonarosa.core.util.logging.Log;
import io.zonarosa.devicetransfer.ClientTask;
import io.zonarosa.messenger.backup.BackupEvent;
import io.zonarosa.messenger.backup.FullBackupExporter;
import io.zonarosa.messenger.crypto.AttachmentSecretProvider;
import io.zonarosa.messenger.database.ZonaRosaDatabase;
import io.zonarosa.messenger.keyvalue.ZonaRosaStore;
import io.zonarosa.messenger.net.DeviceTransferBlockingInterceptor;
import io.zonarosa.messenger.util.RemoteConfig;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Create the backup stream of the old device and sends it over the wire via the output stream.
 * Used in conjunction with {@link io.zonarosa.devicetransfer.DeviceToDeviceTransferService}.
 */
final class OldDeviceClientTask implements ClientTask {

  private static final String TAG = Log.tag(OldDeviceClientTask.class);

  private static final long PROGRESS_UPDATE_THROTTLE = 250;

  private long lastProgressUpdate = 0;

  @Override
  public void run(@NonNull Context context, @NonNull OutputStream outputStream) throws IOException {
    DeviceTransferBlockingInterceptor.getInstance().blockNetwork();

    long start = System.currentTimeMillis();

    EventBus.getDefault().register(this);
    try {
      String passphrase = ZonaRosaStore.account().getAccountEntropyPool().getValue();

      FullBackupExporter.transfer(context,
                                  AttachmentSecretProvider.getInstance(context).getOrCreateAttachmentSecret(),
                                  ZonaRosaDatabase.getBackupDatabase(),
                                  outputStream,
                                  passphrase);
    } catch (Exception e) {
      DeviceTransferBlockingInterceptor.getInstance().unblockNetwork();
      throw e;
    } finally {
      EventBus.getDefault().unregister(this);
    }

    long end = System.currentTimeMillis();
    Log.i(TAG, "Sending took: " + (end - start));
  }

  @Subscribe(threadMode = ThreadMode.POSTING)
  public void onEvent(BackupEvent event) {
    if (event.getType() == BackupEvent.Type.PROGRESS) {
      if (System.currentTimeMillis() > lastProgressUpdate + PROGRESS_UPDATE_THROTTLE) {
        EventBus.getDefault().post(new Status(event.getCount(), event.getEstimatedTotalCount(), event.getCompletionPercentage(), false));
        lastProgressUpdate = System.currentTimeMillis();
      }
    }
  }

  @Override
  public void success() {
    ZonaRosaStore.misc().setOldDeviceTransferLocked(true);
    EventBus.getDefault().post(new Status(0, 0, 0,true));
  }

  public static final class Status {
    private final long    messages;
    private final long    estimatedMessages;
    private final double  completionPercentage;
    private final boolean done;

    public Status(long messages, long estimatedMessages, double completionPercentage, boolean done) {
      this.messages             = messages;
      this.estimatedMessages    = estimatedMessages;
      this.completionPercentage = completionPercentage;
      this.done                 = done;
    }

    public long getMessageCount() {
      return messages;
    }

    public long getEstimatedMessageCount() {
      return estimatedMessages;
    }

    public double getCompletionPercentage() {
      return completionPercentage;
    }

    public boolean isDone() {
      return done;
    }
  }
}
