package io.zonarosa.messenger.components.webrtc;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import io.zonarosa.core.util.PendingIntentFlags;
import io.zonarosa.core.util.logging.Log;
import io.zonarosa.messenger.R;
import io.zonarosa.messenger.components.webrtc.v2.CallIntent;
import io.zonarosa.messenger.notifications.NotificationChannels;
import io.zonarosa.messenger.recipients.Recipient;

/**
 * Utility for showing and hiding safety number change notifications during a group call.
 */
public final class GroupCallSafetyNumberChangeNotificationUtil {

  public static final String TAG = Log.tag(GroupCallSafetyNumberChangeNotificationUtil.class);
  public static final String GROUP_CALLING_NOTIFICATION_TAG = "group_calling";

  private GroupCallSafetyNumberChangeNotificationUtil() {
  }

  public static void showNotification(@NonNull Context context, @NonNull Recipient recipient) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
      Log.w(TAG, "showNotification: Notification permission is not granted.");
      return;
    }

    Intent contentIntent = new Intent(context, CallIntent.getActivityClass());
    contentIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

    PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, contentIntent, PendingIntentFlags.mutable());

    Notification safetyNumberChangeNotification = new NotificationCompat.Builder(context, NotificationChannels.getInstance().CALLS)
                                                                        .setSmallIcon(R.drawable.ic_notification)
                                                                        .setContentTitle(recipient.getDisplayName(context))
                                                                        .setContentText(context.getString(R.string.GroupCallSafetyNumberChangeNotification__someone_has_joined_this_call_with_a_safety_number_that_has_changed))
                                                                        .setStyle(new NotificationCompat.BigTextStyle().bigText(context.getString(R.string.GroupCallSafetyNumberChangeNotification__someone_has_joined_this_call_with_a_safety_number_that_has_changed)))
                                                                        .setContentIntent(pendingIntent)
                                                                        .build();

    NotificationManagerCompat.from(context).notify(GROUP_CALLING_NOTIFICATION_TAG, recipient.hashCode(), safetyNumberChangeNotification);
  }

  public static void cancelNotification(@NonNull Context context, @NonNull Recipient recipient) {
    NotificationManagerCompat.from(context).cancel(GROUP_CALLING_NOTIFICATION_TAG, recipient.hashCode());
  }
}
