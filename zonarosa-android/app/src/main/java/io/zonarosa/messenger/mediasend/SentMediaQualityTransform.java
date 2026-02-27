package io.zonarosa.messenger.mediasend;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import io.zonarosa.core.models.media.Media;
import io.zonarosa.core.models.media.TransformProperties;
import io.zonarosa.messenger.mms.SentMediaQuality;

import java.util.Optional;

import static io.zonarosa.messenger.database.TransformPropertiesUtilKt.transformPropertiesForSentMediaQuality;


/**
 * Add a {@link SentMediaQuality} value for {@link TransformProperties#sentMediaQuality} on the
 * transformed media. Safe to use in a pipeline with other transforms.
 */
public final class SentMediaQualityTransform implements MediaTransform {

  private final SentMediaQuality sentMediaQuality;

  public SentMediaQualityTransform(@NonNull SentMediaQuality sentMediaQuality) {
    this.sentMediaQuality = sentMediaQuality;
  }

  @WorkerThread
  @Override
  public @NonNull Media transform(@NonNull Context context, @NonNull Media media) {
    return new Media(media.getUri(),
                     media.getContentType(),
                     media.getDate(),
                     media.getWidth(),
                     media.getHeight(),
                     media.getSize(),
                     media.getDuration(),
                     media.isBorderless(),
                     media.isVideoGif(),
                     media.getBucketId(),
                     media.getCaption(),
                     transformPropertiesForSentMediaQuality(Optional.ofNullable(media.getTransformProperties()), sentMediaQuality),
                     media.getFileName());
  }
}
