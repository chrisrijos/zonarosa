/**
 * Copyright (C) 2014 ZonaRosa Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.zonarosa.messenger.jobs;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import io.zonarosa.core.util.PendingIntentFlags;
import io.zonarosa.core.util.logging.Log;
import io.zonarosa.messenger.PlayServicesProblemActivity;
import io.zonarosa.messenger.R;
import io.zonarosa.messenger.dependencies.AppDependencies;
import io.zonarosa.messenger.gcm.FcmUtil;
import io.zonarosa.messenger.jobmanager.Job;
import io.zonarosa.messenger.jobmanager.impl.NetworkConstraint;
import io.zonarosa.messenger.keyvalue.ZonaRosaStore;
import io.zonarosa.messenger.net.ZonaRosaNetwork;
import io.zonarosa.messenger.notifications.NotificationChannels;
import io.zonarosa.messenger.notifications.NotificationIds;
import io.zonarosa.messenger.transport.RetryLaterException;
import io.zonarosa.service.api.NetworkResultUtil;
import io.zonarosa.service.api.push.exceptions.NonSuccessfulResponseCodeException;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class FcmRefreshJob extends BaseJob {

  public static final String KEY = "FcmRefreshJob";

  private static final String TAG = Log.tag(FcmRefreshJob.class);

  public FcmRefreshJob() {
    this(new Job.Parameters.Builder()
                           .setQueue("FcmRefreshJob")
                           .addConstraint(NetworkConstraint.KEY)
                           .setMaxAttempts(3)
                           .setLifespan(TimeUnit.HOURS.toMillis(6))
                           .setMaxInstancesForFactory(1)
                           .build());
  }

  private FcmRefreshJob(@NonNull Job.Parameters parameters) {
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
  public void onRun() throws Exception {
    if (!ZonaRosaStore.account().isFcmEnabled()) return;

    Log.i(TAG, "Reregistering FCM...");

    int result = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context);

    if (result != ConnectionResult.SUCCESS) {
      notifyFcmFailure();
    } else {
      Optional<String> token = FcmUtil.getToken(context);

      if (token.isPresent()) {
        String oldToken = ZonaRosaStore.account().getFcmToken();

        if (!token.get().equals(oldToken)) {
          int oldLength = oldToken != null ? oldToken.length() : -1;
          Log.i(TAG, "Token changed. oldLength: " + oldLength + "  newLength: " + token.get().length());
        } else {
          Log.i(TAG, "Token didn't change.");
        }

        NetworkResultUtil.toBasicLegacy(ZonaRosaNetwork.account().setFcmToken(token.get()));
        ZonaRosaStore.account().setFcmToken(token.get());
      } else {
        throw new RetryLaterException(new IOException("Failed to retrieve a token."));
      }
    }
  }

  @Override
  public void onFailure() {
    Log.w(TAG, "FCM reregistration failed after retry attempt exhaustion!");
  }

  @Override
  public boolean onShouldRetry(@NonNull Exception throwable) {
    if (throwable instanceof NonSuccessfulResponseCodeException) return false;
    return true;
  }

  private void notifyFcmFailure() {
    Intent                     intent        = new Intent(context, PlayServicesProblemActivity.class);
    PendingIntent              pendingIntent = PendingIntent.getActivity(context, 1122, intent, PendingIntentFlags.cancelCurrent());
    NotificationCompat.Builder builder       = new NotificationCompat.Builder(context, NotificationChannels.getInstance().FAILURES);

    builder.setSmallIcon(R.drawable.ic_notification);
    builder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(),
                                                      R.drawable.symbol_error_triangle_fill_32));
    builder.setContentTitle(context.getString(R.string.GcmRefreshJob_Permanent_ZonaRosa_communication_failure));
    builder.setContentText(context.getString(R.string.GcmRefreshJob_ZonaRosa_was_unable_to_register_with_Google_Play_Services));
    builder.setTicker(context.getString(R.string.GcmRefreshJob_Permanent_ZonaRosa_communication_failure));
    builder.setVibrate(new long[] {0, 1000});
    builder.setContentIntent(pendingIntent);

    ((NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE))
        .notify(NotificationIds.FCM_FAILURE, builder.build());
  }

  public static final class Factory implements Job.Factory<FcmRefreshJob> {
    @Override
    public @NonNull FcmRefreshJob create(@NonNull Parameters parameters, @Nullable byte[] serializedData) {
      return new FcmRefreshJob(parameters);
    }
  }
}
