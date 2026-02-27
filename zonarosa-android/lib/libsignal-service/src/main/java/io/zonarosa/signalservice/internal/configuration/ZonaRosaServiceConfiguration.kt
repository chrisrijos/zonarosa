package io.zonarosa.service.internal.configuration

import okhttp3.Dns
import okhttp3.Interceptor
import java.util.Optional

/**
 * Defines all network configuration needed to connect to the ZonaRosa service.
 */
@Suppress("ArrayInDataClass") // Using data class for .copy(), don't care about equals/hashcode
data class ZonaRosaServiceConfiguration(
  val zonarosaServiceUrls: Array<ZonaRosaServiceUrl>,
  val zonarosaCdnUrlMap: Map<Int, Array<ZonaRosaCdnUrl>>,
  val zonarosaStorageUrls: Array<ZonaRosaStorageUrl>,
  val zonarosaCdsiUrls: Array<ZonaRosaCdsiUrl>,
  val zonarosaSvr2Urls: Array<ZonaRosaSvr2Url>,
  val networkInterceptors: List<Interceptor>,
  val dns: Optional<Dns>,
  val zonarosaProxy: Optional<ZonaRosaProxy>,
  val systemHttpProxy: Optional<HttpProxy>,
  val zkGroupServerPublicParams: ByteArray,
  val genericServerPublicParams: ByteArray,
  val backupServerPublicParams: ByteArray,
  val censored: Boolean
) {

  /** Convenience operator overload for combining the URL lists. Does not add the other fields together, as those wouldn't make sense.  */
  operator fun plus(other: ZonaRosaServiceConfiguration): ZonaRosaServiceConfiguration {
    return this.copy(
      zonarosaServiceUrls = zonarosaServiceUrls + other.zonarosaServiceUrls,
      zonarosaCdnUrlMap = zonarosaCdnUrlMap + other.zonarosaCdnUrlMap,
      zonarosaStorageUrls = zonarosaStorageUrls + other.zonarosaStorageUrls,
      zonarosaCdsiUrls = zonarosaCdsiUrls + other.zonarosaCdsiUrls,
      zonarosaSvr2Urls = zonarosaSvr2Urls + other.zonarosaSvr2Urls
    )
  }
}
