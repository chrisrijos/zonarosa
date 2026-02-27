package io.zonarosa.messenger.migrations;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;

import io.zonarosa.core.util.SetUtil;
import io.zonarosa.core.util.logging.Log;
import io.zonarosa.messenger.R;
import io.zonarosa.messenger.conversation.NewConversationActivity;
import io.zonarosa.messenger.conversationlist.model.ConversationFilter;
import io.zonarosa.messenger.database.ZonaRosaDatabase;
import io.zonarosa.messenger.database.ThreadTable;
import io.zonarosa.messenger.jobmanager.Job;
import io.zonarosa.messenger.keyvalue.ZonaRosaStore;
import io.zonarosa.messenger.MainActivity;
import io.zonarosa.messenger.notifications.NotificationChannels;
import io.zonarosa.messenger.notifications.NotificationIds;
import io.zonarosa.messenger.recipients.RecipientId;
import io.zonarosa.messenger.util.ZonaRosaPreferences;

import java.util.List;
import java.util.Set;

/**
 * Show a user that contacts are newly available. Only for users that recently installed.
 */
public class UserNotificationMigrationJob extends MigrationJob {

  private static final String TAG = Log.tag(UserNotificationMigrationJob.class);

  public static final String KEY =  "UserNotificationMigration";

  UserNotificationMigrationJob() {
    this(new Parameters.Builder().build());
  }

  private UserNotificationMigrationJob(Parameters parameters) {
    super(parameters);
  }

  @Override
  public @NonNull String getFactoryKey() {
    return KEY;
  }

  @Override
  boolean isUiBlocking() {
    return false;
  }

  @Override
  void performMigration() {
    if (!ZonaRosaStore.account().isRegistered()   ||
        ZonaRosaStore.account().getE164() == null ||
        ZonaRosaStore.account().getAci() == null)
    {
      Log.w(TAG, "Not registered! Skipping.");
      return;
    }

    if (!ZonaRosaStore.settings().isNotifyWhenContactJoinsZonaRosa()) {
      Log.w(TAG, "New contact notifications disabled! Skipping.");
      return;
    }

    if (ZonaRosaPreferences.getFirstInstallVersion(context) < 759) {
      Log.w(TAG, "Install is older than v5.0.8. Skipping.");
      return;
    }

    ThreadTable threadTable = ZonaRosaDatabase.threads();

    int threadCount = threadTable.getUnarchivedConversationListCount(ConversationFilter.OFF, null) +
                      threadTable.getArchivedConversationListCount(ConversationFilter.OFF);

    if (threadCount >= 3) {
      Log.w(TAG, "Already have 3 or more threads. Skipping.");
      return;
    }

    Set<RecipientId>  registered               = ZonaRosaDatabase.recipients().getRegistered();
    List<RecipientId> systemContacts           = ZonaRosaDatabase.recipients().getSystemContacts();
    Set<RecipientId>  registeredSystemContacts = SetUtil.intersection(registered, systemContacts);
    Set<RecipientId>  threadRecipients         = threadTable.getAllThreadRecipients();

    if (threadRecipients.containsAll(registeredSystemContacts)) {
      Log.w(TAG, "Threads already exist for all relevant contacts. Skipping.");
      return;
    }

    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
      Log.w(TAG, "Notification permission is not granted. Skipping.");
      return;
    }

    String message = context.getResources().getQuantityString(R.plurals.UserNotificationMigrationJob_d_contacts_are_on_zonarosa,
                                                              registeredSystemContacts.size(),
                                                              registeredSystemContacts.size());

    Intent        mainActivityIntent    = new Intent(context, MainActivity.class);
    Intent        newConversationIntent = NewConversationActivity.createIntent(context);
    PendingIntent pendingIntent         = TaskStackBuilder.create(context)
                                                          .addNextIntent(mainActivityIntent)
                                                          .addNextIntent(newConversationIntent)
                                                          .getPendingIntent(0, 0);

    Notification notification = new NotificationCompat.Builder(context, NotificationChannels.getInstance().getMessagesChannel())
                                                      .setSmallIcon(R.drawable.ic_notification)
                                                      .setContentText(message)
                                                      .setContentIntent(pendingIntent)
                                                      .build();

    try {
      NotificationManagerCompat.from(context)
                               .notify(NotificationIds.USER_NOTIFICATION_MIGRATION, notification);
    } catch (Throwable t) {
      Log.w(TAG, "Failed to notify!", t);
    }
  }

  @Override
  boolean shouldRetry(@NonNull Exception e) {
    return false;
  }

  public static final class Factory implements Job.Factory<UserNotificationMigrationJob> {

    @Override
    public @NonNull UserNotificationMigrationJob create(@NonNull Parameters parameters, @Nullable byte[] serializedData) {
      return new UserNotificationMigrationJob(parameters);
    }
  }
}
