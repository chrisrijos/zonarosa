package io.zonarosa.messenger.push

import android.content.Context
import android.net.ConnectivityManager
import android.net.ProxyInfo
import android.net.Uri
import androidx.core.content.ContextCompat
import com.google.i18n.phonenumbers.PhoneNumberUtil
import okhttp3.CipherSuite
import okhttp3.ConnectionSpec
import okhttp3.Dns
import okhttp3.Interceptor
import okhttp3.TlsVersion
import io.zonarosa.core.util.Base64
import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.BuildConfig
import io.zonarosa.messenger.keyvalue.SettingsValues
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.net.CustomDns
import io.zonarosa.messenger.net.DeprecatedClientPreventionInterceptor
import io.zonarosa.messenger.net.DeviceTransferBlockingInterceptor
import io.zonarosa.messenger.net.RemoteDeprecationDetectorInterceptor
import io.zonarosa.messenger.net.SequentialDns
import io.zonarosa.messenger.net.StandardUserAgentInterceptor
import io.zonarosa.messenger.net.StaticDns
import io.zonarosa.messenger.net.StorageServiceSizeLoggingInterceptor
import io.zonarosa.service.api.push.TrustStore
import io.zonarosa.service.internal.configuration.HttpProxy
import io.zonarosa.service.internal.configuration.ZonaRosaCdnUrl
import io.zonarosa.service.internal.configuration.ZonaRosaCdsiUrl
import io.zonarosa.service.internal.configuration.ZonaRosaServiceConfiguration
import io.zonarosa.service.internal.configuration.ZonaRosaServiceUrl
import io.zonarosa.service.internal.configuration.ZonaRosaStorageUrl
import io.zonarosa.service.internal.configuration.ZonaRosaSvr2Url
import java.io.IOException
import java.util.Optional

/**
 * Provides a [ZonaRosaServiceConfiguration] to be used with our service layer.
 * If you're looking for a place to start, look at [getConfiguration].
 */
class ZonaRosaServiceNetworkAccess(context: Context) {
  companion object {
    private val TAG = Log.tag(ZonaRosaServiceNetworkAccess::class.java)

    @JvmField
    val DNS: Dns = SequentialDns(
      Dns.SYSTEM,
      CustomDns("1.1.1.1"),
      StaticDns(
        mapOf(
          BuildConfig.ZONAROSA_URL.stripProtocol() to BuildConfig.ZONAROSA_SERVICE_IPS.toSet(),
          BuildConfig.STORAGE_URL.stripProtocol() to BuildConfig.ZONAROSA_STORAGE_IPS.toSet(),
          BuildConfig.ZONAROSA_CDN_URL.stripProtocol() to BuildConfig.ZONAROSA_CDN_IPS.toSet(),
          BuildConfig.ZONAROSA_CDN2_URL.stripProtocol() to BuildConfig.ZONAROSA_CDN2_IPS.toSet(),
          BuildConfig.ZONAROSA_CDN3_URL.stripProtocol() to BuildConfig.ZONAROSA_CDN3_IPS.toSet(),
          BuildConfig.ZONAROSA_SFU_URL.stripProtocol() to BuildConfig.ZONAROSA_SFU_IPS.toSet(),
          BuildConfig.CONTENT_PROXY_HOST.stripProtocol() to BuildConfig.ZONAROSA_CONTENT_PROXY_IPS.toSet(),
          BuildConfig.ZONAROSA_CDSI_URL.stripProtocol() to BuildConfig.ZONAROSA_CDSI_IPS.toSet(),
          BuildConfig.ZONAROSA_SVR2_URL.stripProtocol() to BuildConfig.ZONAROSA_SVR2_IPS.toSet()
        )
      )
    )

    private fun String.stripProtocol(): String {
      return this.removePrefix("https://")
    }

    private const val COUNTRY_CODE_EGYPT = 20
    private const val COUNTRY_CODE_UAE = 971
    private const val COUNTRY_CODE_OMAN = 968
    private const val COUNTRY_CODE_QATAR = 974
    private const val COUNTRY_CODE_IRAN = 98
    private const val COUNTRY_CODE_CUBA = 53
    private const val COUNTRY_CODE_UZBEKISTAN = 998
    private const val COUNTRY_CODE_VENEZUELA = 58
    private const val COUNTRY_CODE_PAKISTAN = 92

    private const val G_HOST = "reflector-nrgwuv7kwq-uc.a.run.app"
    private const val F_SERVICE_HOST = "chat-zonarosa.global.ssl.fastly.net"
    private const val F_STORAGE_HOST = "storage.zonarosa.io.global.prod.fastly.net"
    private const val F_CDN_HOST = "cdn.zonarosa.io.global.prod.fastly.net"
    private const val F_CDN2_HOST = "cdn2.zonarosa.io.global.prod.fastly.net"
    private const val F_CDN3_HOST = "cdn3-zonarosa.global.ssl.fastly.net"
    private const val F_CDSI_HOST = "cdsi-zonarosa.global.ssl.fastly.net"
    private const val F_SVR2_HOST = "svr2-zonarosa.global.ssl.fastly.net"

    private val GMAPS_CONNECTION_SPEC = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
      .tlsVersions(TlsVersion.TLS_1_2)
      .cipherSuites(
        CipherSuite.TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256,
        CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
        CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384,
        CipherSuite.TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256,
        CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
        CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,
        CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA,
        CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA,
        CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA,
        CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA,
        CipherSuite.TLS_RSA_WITH_AES_128_GCM_SHA256,
        CipherSuite.TLS_RSA_WITH_AES_256_GCM_SHA384,
        CipherSuite.TLS_RSA_WITH_AES_128_CBC_SHA,
        CipherSuite.TLS_RSA_WITH_AES_256_CBC_SHA
      )
      .supportsTlsExtensions(true)
      .build()

    private val GMAIL_CONNECTION_SPEC = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
      .tlsVersions(TlsVersion.TLS_1_2)
      .cipherSuites(
        CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
        CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
        CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA,
        CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA,
        CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA,
        CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA,
        CipherSuite.TLS_RSA_WITH_AES_128_GCM_SHA256,
        CipherSuite.TLS_RSA_WITH_AES_128_CBC_SHA,
        CipherSuite.TLS_RSA_WITH_AES_256_CBC_SHA
      )
      .supportsTlsExtensions(true)
      .build()

    private val PLAY_CONNECTION_SPEC = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
      .tlsVersions(TlsVersion.TLS_1_2)
      .cipherSuites(
        CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
        CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
        CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA,
        CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA,
        CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA,
        CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA,
        CipherSuite.TLS_RSA_WITH_AES_128_GCM_SHA256,
        CipherSuite.TLS_RSA_WITH_AES_128_CBC_SHA,
        CipherSuite.TLS_RSA_WITH_AES_256_CBC_SHA
      )
      .supportsTlsExtensions(true)
      .build()

    private val APP_CONNECTION_SPEC = ConnectionSpec.MODERN_TLS

    @Suppress("DEPRECATION")
    private fun getSystemHttpProxy(context: Context): HttpProxy? {
      val connectivityManager = ContextCompat.getSystemService(context, ConnectivityManager::class.java) ?: return null

      val proxyInfo = connectivityManager
        .activeNetwork
        ?.let { connectivityManager.getLinkProperties(it)?.httpProxy }

      return proxyInfo.toApplicableSystemHttpProxy()
    }

    fun ProxyInfo?.toApplicableSystemHttpProxy(): HttpProxy? {
      return this
        ?.takeIf { !it.exclusionList.contains(BuildConfig.ZONAROSA_URL.stripProtocol()) }
        // NB: Edit carefully, dear reader, as the line below is written from hard won experience.
        // It turns out, that despite being documented *nowhere*, if a PAC file is set
        //   as the system proxy, proxyInfo.host will return "localhost" and proxyInfo.port
        //   will return -1.
        // I learnt this by reading the AOSP source code for ProxyInfo:
        //   https://android.googlesource.com/platform/frameworks/base/+/4696ee4/core/java/android/net/ProxyInfo.java#107
        // So, if we do not explicitly check that a PAC file is not set, the proxy
        //   we pass to libzonarosa may be syntactically invalid, and the user may be
        //   rendered unable to connect.
        ?.takeIf { it.pacFileUrl == Uri.EMPTY }
        ?.let { proxy -> HttpProxy(proxy.host, proxy.port) }
    }
  }

  private val serviceTrustStore: TrustStore = ZonaRosaServiceTrustStore(context)
  private val gTrustStore: TrustStore = DomainFrontingTrustStore(context)
  private val fTrustStore: TrustStore = DomainFrontingDigicertTrustStore(context)

  private val interceptors: List<Interceptor> = listOf(
    StandardUserAgentInterceptor(),
    StorageServiceSizeLoggingInterceptor(),
    RemoteDeprecationDetectorInterceptor(this::getConfiguration),
    DeprecatedClientPreventionInterceptor(),
    DeviceTransferBlockingInterceptor.getInstance()
  )

  private val zkGroupServerPublicParams: ByteArray = try {
    Base64.decode(BuildConfig.ZKGROUP_SERVER_PUBLIC_PARAMS)
  } catch (e: IOException) {
    throw AssertionError(e)
  }

  private val genericServerPublicParams: ByteArray = try {
    Base64.decode(BuildConfig.GENERIC_SERVER_PUBLIC_PARAMS)
  } catch (e: IOException) {
    throw AssertionError(e)
  }

  private val backupServerPublicParams: ByteArray = try {
    Base64.decode(BuildConfig.BACKUP_SERVER_PUBLIC_PARAMS)
  } catch (e: IOException) {
    throw AssertionError(e)
  }

  private val baseGHostConfigs: List<HostConfig> = listOf(
    HostConfig("https://www.google.com", G_HOST, GMAIL_CONNECTION_SPEC),
    HostConfig("https://android.clients.google.com", G_HOST, PLAY_CONNECTION_SPEC),
    HostConfig("https://clients3.google.com", G_HOST, GMAPS_CONNECTION_SPEC),
    HostConfig("https://clients4.google.com", G_HOST, GMAPS_CONNECTION_SPEC),
    HostConfig("https://googlemail.com", G_HOST, GMAIL_CONNECTION_SPEC)
  )

  private val fUrls = arrayOf("https://github.githubassets.com", "https://pinterest.com", "https://www.redditstatic.com")

  private val fConfig: ZonaRosaServiceConfiguration = ZonaRosaServiceConfiguration(
    zonarosaServiceUrls = fUrls.map { ZonaRosaServiceUrl(it, F_SERVICE_HOST, fTrustStore, APP_CONNECTION_SPEC) }.toTypedArray(),
    zonarosaCdnUrlMap = mapOf(
      0 to fUrls.map { ZonaRosaCdnUrl(it, F_CDN_HOST, fTrustStore, APP_CONNECTION_SPEC) }.toTypedArray(),
      2 to fUrls.map { ZonaRosaCdnUrl(it, F_CDN2_HOST, fTrustStore, APP_CONNECTION_SPEC) }.toTypedArray(),
      3 to fUrls.map { ZonaRosaCdnUrl(it, F_CDN3_HOST, fTrustStore, APP_CONNECTION_SPEC) }.toTypedArray()
    ),
    zonarosaStorageUrls = fUrls.map { ZonaRosaStorageUrl(it, F_STORAGE_HOST, fTrustStore, APP_CONNECTION_SPEC) }.toTypedArray(),
    zonarosaCdsiUrls = fUrls.map { ZonaRosaCdsiUrl(it, F_CDSI_HOST, fTrustStore, APP_CONNECTION_SPEC) }.toTypedArray(),
    zonarosaSvr2Urls = fUrls.map { ZonaRosaSvr2Url(it, fTrustStore, F_SVR2_HOST, APP_CONNECTION_SPEC) }.toTypedArray(),
    networkInterceptors = interceptors,
    dns = Optional.of(DNS),
    zonarosaProxy = Optional.empty(),
    systemHttpProxy = Optional.empty(),
    zkGroupServerPublicParams = zkGroupServerPublicParams,
    genericServerPublicParams = genericServerPublicParams,
    backupServerPublicParams = backupServerPublicParams,
    censored = true
  )

  private val censorshipConfiguration: Map<Int, ZonaRosaServiceConfiguration> = mapOf(
    COUNTRY_CODE_EGYPT to buildGConfiguration(
      listOf(HostConfig("https://www.google.com.eg", G_HOST, GMAIL_CONNECTION_SPEC)) + baseGHostConfigs
    ),
    COUNTRY_CODE_UAE to buildGConfiguration(
      listOf(HostConfig("https://www.google.ae", G_HOST, GMAIL_CONNECTION_SPEC)) + baseGHostConfigs
    ),
    COUNTRY_CODE_OMAN to buildGConfiguration(
      listOf(HostConfig("https://www.google.com.om", G_HOST, GMAIL_CONNECTION_SPEC)) + baseGHostConfigs
    ),
    COUNTRY_CODE_QATAR to buildGConfiguration(
      listOf(HostConfig("https://www.google.com.qa", G_HOST, GMAIL_CONNECTION_SPEC)) + baseGHostConfigs
    ),
    COUNTRY_CODE_UZBEKISTAN to buildGConfiguration(
      listOf(HostConfig("https://www.google.co.uz", G_HOST, GMAIL_CONNECTION_SPEC)) + baseGHostConfigs
    ),
    COUNTRY_CODE_VENEZUELA to buildGConfiguration(
      listOf(HostConfig("https://www.google.co.ve", G_HOST, GMAIL_CONNECTION_SPEC)) + baseGHostConfigs
    ),
    COUNTRY_CODE_PAKISTAN to buildGConfiguration(
      listOf(HostConfig("https://www.google.com.pk", G_HOST, GMAIL_CONNECTION_SPEC)) + baseGHostConfigs
    ),
    COUNTRY_CODE_IRAN to fConfig,
    COUNTRY_CODE_CUBA to fConfig
  )

  private val defaultCensoredConfiguration: ZonaRosaServiceConfiguration = buildGConfiguration(baseGHostConfigs) + fConfig

  private val defaultCensoredCountryCodes: Set<Int> = setOf(
    COUNTRY_CODE_EGYPT,
    COUNTRY_CODE_UAE,
    COUNTRY_CODE_OMAN,
    COUNTRY_CODE_QATAR,
    COUNTRY_CODE_IRAN,
    COUNTRY_CODE_CUBA,
    COUNTRY_CODE_UZBEKISTAN,
    COUNTRY_CODE_VENEZUELA,
    COUNTRY_CODE_PAKISTAN
  )

  val uncensoredConfiguration: ZonaRosaServiceConfiguration = ZonaRosaServiceConfiguration(
    zonarosaServiceUrls = arrayOf(ZonaRosaServiceUrl(BuildConfig.ZONAROSA_URL, serviceTrustStore)),
    zonarosaCdnUrlMap = mapOf(
      0 to arrayOf(ZonaRosaCdnUrl(BuildConfig.ZONAROSA_CDN_URL, serviceTrustStore)),
      2 to arrayOf(ZonaRosaCdnUrl(BuildConfig.ZONAROSA_CDN2_URL, serviceTrustStore)),
      3 to arrayOf(ZonaRosaCdnUrl(BuildConfig.ZONAROSA_CDN3_URL, serviceTrustStore))
    ),
    zonarosaStorageUrls = arrayOf(ZonaRosaStorageUrl(BuildConfig.STORAGE_URL, serviceTrustStore)),
    zonarosaCdsiUrls = arrayOf(ZonaRosaCdsiUrl(BuildConfig.ZONAROSA_CDSI_URL, serviceTrustStore)),
    zonarosaSvr2Urls = arrayOf(ZonaRosaSvr2Url(BuildConfig.ZONAROSA_SVR2_URL, serviceTrustStore)),
    networkInterceptors = interceptors,
    dns = Optional.of(DNS),
    zonarosaProxy = if (ZonaRosaStore.proxy.isProxyEnabled) Optional.ofNullable(ZonaRosaStore.proxy.proxy) else Optional.empty(),
    systemHttpProxy = Optional.ofNullable(getSystemHttpProxy(context)),
    zkGroupServerPublicParams = zkGroupServerPublicParams,
    genericServerPublicParams = genericServerPublicParams,
    backupServerPublicParams = backupServerPublicParams,
    censored = false
  )

  fun getConfiguration(): ZonaRosaServiceConfiguration {
    return getConfiguration(ZonaRosaStore.account.e164)
  }

  fun getConfiguration(e164: String?): ZonaRosaServiceConfiguration {
    if (e164 == null || ZonaRosaStore.proxy.isProxyEnabled) {
      return uncensoredConfiguration
    }

    val countryCode: Int = PhoneNumberUtil.getInstance().parse(e164, null).countryCode

    return when (ZonaRosaStore.settings.censorshipCircumventionEnabled) {
      SettingsValues.CensorshipCircumventionEnabled.ENABLED -> {
        censorshipConfiguration[countryCode] ?: defaultCensoredConfiguration
      }

      SettingsValues.CensorshipCircumventionEnabled.DISABLED -> {
        uncensoredConfiguration
      }

      SettingsValues.CensorshipCircumventionEnabled.DEFAULT -> {
        if (defaultCensoredCountryCodes.contains(countryCode)) {
          censorshipConfiguration[countryCode] ?: defaultCensoredConfiguration
        } else {
          uncensoredConfiguration
        }
      }
    }
  }

  fun isCensored(): Boolean {
    return isCensored(ZonaRosaStore.account.e164)
  }

  fun isCensored(number: String?): Boolean {
    return getConfiguration(number) != uncensoredConfiguration
  }

  fun isCountryCodeCensoredByDefault(countryCode: Int): Boolean {
    return defaultCensoredCountryCodes.contains(countryCode)
  }

  private fun buildGConfiguration(
    hostConfigs: List<HostConfig>
  ): ZonaRosaServiceConfiguration {
    val serviceUrls: Array<ZonaRosaServiceUrl> = hostConfigs.map { ZonaRosaServiceUrl("${it.baseUrl}/service", it.host, gTrustStore, it.connectionSpec) }.toTypedArray()
    val cdnUrls: Array<ZonaRosaCdnUrl> = hostConfigs.map { ZonaRosaCdnUrl("${it.baseUrl}/cdn", it.host, gTrustStore, it.connectionSpec) }.toTypedArray()
    val cdn2Urls: Array<ZonaRosaCdnUrl> = hostConfigs.map { ZonaRosaCdnUrl("${it.baseUrl}/cdn2", it.host, gTrustStore, it.connectionSpec) }.toTypedArray()
    val cdn3Urls: Array<ZonaRosaCdnUrl> = hostConfigs.map { ZonaRosaCdnUrl("${it.baseUrl}/cdn3", it.host, gTrustStore, it.connectionSpec) }.toTypedArray()
    val storageUrls: Array<ZonaRosaStorageUrl> = hostConfigs.map { ZonaRosaStorageUrl("${it.baseUrl}/storage", it.host, gTrustStore, it.connectionSpec) }.toTypedArray()
    val cdsiUrls: Array<ZonaRosaCdsiUrl> = hostConfigs.map { ZonaRosaCdsiUrl("${it.baseUrl}/cdsi", it.host, gTrustStore, it.connectionSpec) }.toTypedArray()
    val svr2Urls: Array<ZonaRosaSvr2Url> = hostConfigs.map { ZonaRosaSvr2Url("${it.baseUrl}/svr2", gTrustStore, it.host, it.connectionSpec) }.toTypedArray()

    return ZonaRosaServiceConfiguration(
      zonarosaServiceUrls = serviceUrls,
      zonarosaCdnUrlMap = mapOf(
        0 to cdnUrls,
        2 to cdn2Urls,
        3 to cdn3Urls
      ),
      zonarosaStorageUrls = storageUrls,
      zonarosaCdsiUrls = cdsiUrls,
      zonarosaSvr2Urls = svr2Urls,
      networkInterceptors = interceptors,
      dns = Optional.of(DNS),
      zonarosaProxy = Optional.empty(),
      systemHttpProxy = Optional.empty(),
      zkGroupServerPublicParams = zkGroupServerPublicParams,
      genericServerPublicParams = genericServerPublicParams,
      backupServerPublicParams = backupServerPublicParams,
      censored = true
    )
  }

  private data class HostConfig(val baseUrl: String, val host: String, val connectionSpec: ConnectionSpec)
}
