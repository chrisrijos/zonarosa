package io.zonarosa.messenger.mediasend;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import io.zonarosa.core.models.media.Media;

public interface MediaTransform {

  @WorkerThread
  @NonNull Media transform(@NonNull Context context, @NonNull Media media);
}
