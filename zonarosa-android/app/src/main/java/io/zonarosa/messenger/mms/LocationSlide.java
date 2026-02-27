package io.zonarosa.messenger.mms;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import io.zonarosa.messenger.components.location.ZonaRosaPlace;

import java.util.Optional;


public class LocationSlide extends ImageSlide {

  @NonNull
  private final ZonaRosaPlace place;

  public LocationSlide(@NonNull  Context context, @NonNull  Uri uri, long size, @NonNull ZonaRosaPlace place)
  {
    super(context, uri, size, 0, 0, null);
    this.place = place;
  }

  @Override
  @NonNull
  public Optional<String> getBody() {
    return Optional.of(place.getDescription());
  }

  @NonNull
  public ZonaRosaPlace getPlace() {
    return place;
  }

  @Override
  public boolean hasLocation() {
    return true;
  }

}
