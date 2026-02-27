package io.zonarosa.service.api;

public class ContentTooLargeException extends IllegalStateException {
  public ContentTooLargeException(long size, String details) {
    super("Too large! Size: " + size + " bytes. Details: " + details);
  }
}
