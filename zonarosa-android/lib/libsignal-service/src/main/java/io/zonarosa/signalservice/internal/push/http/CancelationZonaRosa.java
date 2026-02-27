package io.zonarosa.service.internal.push.http;

/**
 * Used to communicate to observers whether or not something is canceled.
 */
public interface CancelationZonaRosa {
  boolean isCanceled();
}
