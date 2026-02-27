package io.zonarosa.websocket;

/**
 * Class containing constants and shared logic for headers used in websocket upgrade requests.
 */
public class WebsocketHeaders {
  public final static String X_ZONAROSA_RECEIVE_STORIES = "X-ZonaRosa-Receive-Stories";

  public static boolean parseReceiveStoriesHeader(String s) {
    return "true".equals(s);
  }
}
