package io.zonarosa.messenger.longmessage;

import android.content.Context;
import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import io.zonarosa.core.util.concurrent.ZonaRosaExecutors;
import io.zonarosa.core.util.logging.Log;
import io.zonarosa.messenger.conversation.ConversationMessage;
import io.zonarosa.messenger.database.MessageTable;
import io.zonarosa.messenger.database.ZonaRosaDatabase;
import io.zonarosa.messenger.database.model.MmsMessageRecord;

import java.util.Optional;

class LongMessageRepository {

  private final static String TAG = Log.tag(LongMessageRepository.class);

  private final MessageTable messageTable;

  LongMessageRepository() {
    this.messageTable = ZonaRosaDatabase.messages();
  }

  void getMessage(@NonNull Context context, long messageId, @NonNull Callback<Optional<LongMessage>> callback) {
    ZonaRosaExecutors.BOUNDED.execute(() -> {
      callback.onComplete(getMmsLongMessage(context, messageTable, messageId));
    });
  }

  @WorkerThread
  private Optional<LongMessage> getMmsLongMessage(@NonNull Context context, @NonNull MessageTable mmsDatabase, long messageId) {
    Optional<MmsMessageRecord> record = getMmsMessage(mmsDatabase, messageId);
    if (record.isPresent()) {
      final ConversationMessage resolvedMessage = LongMessageResolveerKt.resolveBody(record.get(), context);
      return  Optional.of(new LongMessage(resolvedMessage));
    } else {
      return Optional.empty();
    }
  }

  @WorkerThread
  private Optional<MmsMessageRecord> getMmsMessage(@NonNull MessageTable mmsDatabase, long messageId) {
    try (Cursor cursor = mmsDatabase.getMessageCursor(messageId)) {
      return Optional.ofNullable((MmsMessageRecord) MessageTable.mmsReaderFor(cursor).getNext());
    }
  }


  interface Callback<T> {
    void onComplete(T result);
  }
}
