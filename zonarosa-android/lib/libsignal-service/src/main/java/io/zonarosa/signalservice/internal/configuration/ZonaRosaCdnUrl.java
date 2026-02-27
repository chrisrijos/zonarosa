package io.zonarosa.service.internal.configuration;


import io.zonarosa.service.api.push.TrustStore;

import okhttp3.ConnectionSpec;

public class ZonaRosaCdnUrl extends ZonaRosaUrl {
  public ZonaRosaCdnUrl(String url, TrustStore trustStore) {
    super(url, trustStore);
  }

  public ZonaRosaCdnUrl(String url, String hostHeader, TrustStore trustStore, ConnectionSpec connectionSpec) {
    super(url, hostHeader, trustStore, connectionSpec);
  }
}
