package io.zonarosa.messenger.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import io.zonarosa.messenger.dependencies.AppDependencies;
import io.zonarosa.messenger.jobs.MessageFetchJob;

public class BootReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    AppDependencies.getJobManager().add(new MessageFetchJob());
  }
}
