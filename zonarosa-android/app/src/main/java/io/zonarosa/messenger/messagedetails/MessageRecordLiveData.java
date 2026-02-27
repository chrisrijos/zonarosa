package io.zonarosa.messenger.messagedetails;

import androidx.annotation.WorkerThread;
import androidx.lifecycle.LiveData;

import io.zonarosa.core.util.concurrent.ZonaRosaExecutors;
import io.zonarosa.messenger.database.DatabaseObserver;
import io.zonarosa.messenger.database.NoSuchMessageException;
import io.zonarosa.messenger.database.ZonaRosaDatabase;
import io.zonarosa.messenger.database.model.MessageId;
import io.zonarosa.messenger.database.model.MessageRecord;
import io.zonarosa.messenger.dependencies.AppDependencies;

final class MessageRecordLiveData extends LiveData<MessageRecord> {

  private final DatabaseObserver.Observer observer;
  private final MessageId                 messageId;

  MessageRecordLiveData(MessageId messageId) {
    this.messageId = messageId;
    this.observer  = this::retrieveMessageRecordActual;
  }

  @Override
  protected void onActive() {
    ZonaRosaExecutors.BOUNDED_IO.execute(this::retrieveMessageRecordActual);
  }

  @Override
  protected void onInactive() {
    AppDependencies.getDatabaseObserver().unregisterObserver(observer);
  }

  @WorkerThread
  private synchronized void retrieveMessageRecordActual() {
    try {
      MessageRecord record = ZonaRosaDatabase.messages().getMessageRecord(messageId.getId());

      if (record.isPaymentNotification()) {
        record = ZonaRosaDatabase.payments().updateMessageWithPayment(record);
      }

      postValue(record);
      AppDependencies.getDatabaseObserver().registerVerboseConversationObserver(record.getThreadId(), observer);
    } catch (NoSuchMessageException ignored) {
      postValue(null);
    }
  }
}
