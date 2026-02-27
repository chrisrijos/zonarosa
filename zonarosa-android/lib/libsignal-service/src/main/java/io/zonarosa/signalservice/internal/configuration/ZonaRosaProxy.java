package io.zonarosa.service.internal.configuration;

public class ZonaRosaProxy {
  private final String host;
  private final int    port;

  public ZonaRosaProxy(String host, int port) {
    this.host = host;
    this.port = port;
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }
}
