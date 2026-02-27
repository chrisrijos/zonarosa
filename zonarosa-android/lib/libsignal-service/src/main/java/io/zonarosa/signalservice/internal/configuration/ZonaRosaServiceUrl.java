package io.zonarosa.service.internal.configuration;


import io.zonarosa.service.api.push.TrustStore;

import okhttp3.ConnectionSpec;

public class ZonaRosaServiceUrl extends ZonaRosaUrl {

  public ZonaRosaServiceUrl(String url, TrustStore trustStore) {
    super(url, trustStore);
  }

  public ZonaRosaServiceUrl(String url, String hostHeader, TrustStore trustStore, ConnectionSpec connectionSpec) {
    super(url, hostHeader, trustStore, connectionSpec);
  }
}
