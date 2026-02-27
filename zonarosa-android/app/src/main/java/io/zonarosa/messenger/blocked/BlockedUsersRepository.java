package io.zonarosa.messenger.blocked;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;

import io.zonarosa.core.util.concurrent.ZonaRosaExecutors;
import io.zonarosa.core.util.logging.Log;
import io.zonarosa.messenger.database.ZonaRosaDatabase;
import io.zonarosa.messenger.database.model.RecipientRecord;
import io.zonarosa.messenger.groups.GroupChangeBusyException;
import io.zonarosa.messenger.groups.GroupChangeFailedException;
import io.zonarosa.messenger.recipients.Recipient;
import io.zonarosa.messenger.recipients.RecipientId;
import io.zonarosa.messenger.recipients.RecipientUtil;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

class BlockedUsersRepository {

  private static final String TAG = Log.tag(BlockedUsersRepository.class);

  private final Context context;

  BlockedUsersRepository(@NonNull Context context) {
    this.context = context;
  }

  void getBlocked(@NonNull Consumer<List<Recipient>> blockedUsers) {
    ZonaRosaExecutors.BOUNDED.execute(() -> {
      List<RecipientRecord> records    = ZonaRosaDatabase.recipients().getBlocked();
      List<Recipient>       recipients = records.stream()
                                                .map((record) -> Recipient.resolved(record.getId()))
                                                .collect(Collectors.toList());
      blockedUsers.accept(recipients);
    });
  }

  void block(@NonNull RecipientId recipientId, @NonNull Runnable success, @NonNull Runnable failure) {
    ZonaRosaExecutors.BOUNDED.execute(() -> {
      try {
        RecipientUtil.block(context, Recipient.resolved(recipientId));
        success.run();
      } catch (IOException | GroupChangeFailedException | GroupChangeBusyException e) {
        Log.w(TAG, "block: failed to block recipient: ", e);
        failure.run();
      }
    });
  }

  void createAndBlock(@NonNull String number, @NonNull Runnable success) {
    ZonaRosaExecutors.BOUNDED.execute(() -> {
      Recipient recipient = Recipient.external(number);
      if (recipient != null) {
        RecipientUtil.blockNonGroup(context, recipient);
      } else {
        Log.w(TAG, "Failed to create Recipient for number! Invalid input.");
      }
      success.run();
    });
  }

  void unblock(@NonNull RecipientId recipientId, @NonNull Runnable success) {
    ZonaRosaExecutors.BOUNDED.execute(() -> {
      RecipientUtil.unblock(Recipient.resolved(recipientId));
      success.run();
    });
  }
}
