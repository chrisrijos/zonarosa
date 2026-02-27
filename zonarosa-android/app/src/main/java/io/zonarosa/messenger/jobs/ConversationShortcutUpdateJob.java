package io.zonarosa.messenger.jobs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.zonarosa.core.util.logging.Log;
import io.zonarosa.messenger.database.ZonaRosaDatabase;
import io.zonarosa.messenger.database.ThreadTable;
import io.zonarosa.messenger.database.model.ThreadRecord;
import io.zonarosa.messenger.dependencies.AppDependencies;
import io.zonarosa.messenger.jobmanager.Job;
import io.zonarosa.messenger.keyvalue.ZonaRosaStore;
import io.zonarosa.messenger.recipients.Recipient;
import io.zonarosa.messenger.transport.RetryLaterException;
import io.zonarosa.messenger.util.ConversationUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * On some devices, interacting with the ShortcutManager can take a very long time (several seconds).
 * So, we interact with it in a job instead, and keep it in one queue so it can't starve the other
 * job runners.
 */
public class ConversationShortcutUpdateJob extends BaseJob {

  private static final String TAG = Log.tag(ConversationShortcutUpdateJob.class);

  public static final String KEY = "ConversationShortcutUpdateJob";

  public static void enqueue() {
    AppDependencies.getJobManager().add(new ConversationShortcutUpdateJob());
  }

  private ConversationShortcutUpdateJob() {
    this(new Parameters.Builder()
                       .setQueue("ConversationShortcutUpdateJob")
                       .setLifespan(TimeUnit.MINUTES.toMillis(15))
                       .setMaxInstancesForFactory(1)
                       .setGlobalPriority(Parameters.PRIORITY_LOW)
                       .build());
  }

  private ConversationShortcutUpdateJob(@NonNull Parameters parameters) {
    super(parameters);
  }

  @Override
  public @Nullable byte[] serialize() {
    return null;
  }

  @Override
  public @NonNull String getFactoryKey() {
    return KEY;
  }

  @Override
  protected void onRun() throws Exception {
    if (ZonaRosaStore.settings().getScreenLockEnabled()) {
      Log.i(TAG, "Screen lock enabled. Clearing shortcuts.");
      ConversationUtil.clearAllShortcuts(context);
      return;
    }

    if (ZonaRosaStore.account().getAci() == null) {
      Log.i(TAG, "Need ACI for group shortcuts");
      return;
    }

    ThreadTable threadTable  = ZonaRosaDatabase.threads();
    int         maxShortcuts = ConversationUtil.getMaxShortcuts(context);
    List<Recipient> ranked         = new ArrayList<>(maxShortcuts);

    try (ThreadTable.Reader reader = threadTable.readerFor(threadTable.getRecentConversationList(maxShortcuts, false, false, false, true, true, false))) {
      ThreadRecord record;
      while ((record = reader.getNext()) != null) {
        ranked.add(record.getRecipient().resolve());
      }
    }

    boolean success = ConversationUtil.setActiveShortcuts(context, ranked);

    if (!success) {
      throw new RetryLaterException();
    }

    ConversationUtil.removeLongLivedShortcuts(context, threadTable.getArchivedRecipients());
  }

  @Override
  protected boolean onShouldRetry(@NonNull Exception e) {
    return e instanceof RetryLaterException;
  }

  @Override
  public void onFailure() {
  }

  public static class Factory implements Job.Factory<ConversationShortcutUpdateJob> {
    @Override
    public @NonNull ConversationShortcutUpdateJob create(@NonNull Parameters parameters, @Nullable byte[] serializedData) {
      return new ConversationShortcutUpdateJob(parameters);
    }
  }
}
