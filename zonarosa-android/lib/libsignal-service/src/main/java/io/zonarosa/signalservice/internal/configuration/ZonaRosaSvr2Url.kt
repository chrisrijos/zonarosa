package io.zonarosa.service.internal.configuration

import okhttp3.ConnectionSpec
import io.zonarosa.service.api.push.TrustStore

/**
 * Configuration for reach the SVR2 service.
 */
class ZonaRosaSvr2Url(
  url: String,
  trustStore: TrustStore,
  hostHeader: String? = null,
  connectionSpec: ConnectionSpec? = null
) : ZonaRosaUrl(url, hostHeader, trustStore, connectionSpec)
