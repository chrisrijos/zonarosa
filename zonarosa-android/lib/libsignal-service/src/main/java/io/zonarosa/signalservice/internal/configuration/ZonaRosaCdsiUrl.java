package io.zonarosa.service.internal.configuration;


import io.zonarosa.service.api.push.TrustStore;

import okhttp3.ConnectionSpec;

public class ZonaRosaCdsiUrl extends ZonaRosaUrl {

  public ZonaRosaCdsiUrl(String url, TrustStore trustStore) {
    super(url, trustStore);
  }

  public ZonaRosaCdsiUrl(String url, String hostHeader, TrustStore trustStore, ConnectionSpec connectionSpec) {
    super(url, hostHeader, trustStore, connectionSpec);
  }
}
