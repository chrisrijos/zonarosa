package io.zonarosa.messenger.revealable;

import android.content.Context;

import androidx.annotation.NonNull;

import io.zonarosa.core.util.concurrent.ZonaRosaExecutors;
import io.zonarosa.core.util.logging.Log;
import io.zonarosa.messenger.database.MessageTable;
import io.zonarosa.messenger.database.NoSuchMessageException;
import io.zonarosa.messenger.database.ZonaRosaDatabase;
import io.zonarosa.messenger.database.model.MmsMessageRecord;
import io.zonarosa.messenger.dependencies.AppDependencies;
import io.zonarosa.messenger.jobs.MultiDeviceViewedUpdateJob;
import io.zonarosa.messenger.jobs.SendViewedReceiptJob;

import java.util.Collections;
import java.util.Optional;

class ViewOnceMessageRepository {

  private static final String TAG = Log.tag(ViewOnceMessageRepository.class);

  private final MessageTable mmsDatabase;

  ViewOnceMessageRepository(@NonNull Context context) {
    this.mmsDatabase = ZonaRosaDatabase.messages();
  }

  void getMessage(long messageId, @NonNull Callback<Optional<MmsMessageRecord>> callback) {
    ZonaRosaExecutors.BOUNDED.execute(() -> {
      try {
        MmsMessageRecord record = (MmsMessageRecord) mmsDatabase.getMessageRecord(messageId);

        MessageTable.MarkedMessageInfo info = mmsDatabase.setIncomingMessageViewed(record.getId());
        if (info != null) {
          AppDependencies.getJobManager().add(new SendViewedReceiptJob(record.getThreadId(),
                                                                       info.getSyncMessageId().getRecipientId(),
                                                                       info.getSyncMessageId().getTimetamp(),
                                                                       info.getMessageId()));
          MultiDeviceViewedUpdateJob.enqueue(Collections.singletonList(info.getSyncMessageId()));
        }

        callback.onComplete(Optional.ofNullable(record));
      } catch (NoSuchMessageException e) {
        callback.onComplete(Optional.empty());
      }
    });
  }

  interface Callback<T> {
    void onComplete(T result);
  }
}
