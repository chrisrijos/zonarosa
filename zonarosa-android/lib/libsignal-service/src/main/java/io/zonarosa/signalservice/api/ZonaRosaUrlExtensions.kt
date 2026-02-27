/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.api

import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import io.zonarosa.service.api.push.TrustStore
import io.zonarosa.service.api.util.Tls12SocketFactory
import io.zonarosa.service.api.util.TlsProxySocketFactory
import io.zonarosa.service.internal.configuration.ZonaRosaServiceConfiguration
import io.zonarosa.service.internal.configuration.ZonaRosaUrl
import io.zonarosa.service.internal.util.BlacklistingTrustManager
import io.zonarosa.service.internal.util.Util
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

/**
 * Select a a URL at random to use.
 */
fun <T : ZonaRosaUrl> Array<T>.chooseUrl(): T {
  return this[(Math.random() * size).toInt()]
}

/**
 * Build and configure an [OkHttpClient] as defined by the target [ZonaRosaUrl] and provided [configuration].
 */
fun <T : ZonaRosaUrl> T.buildOkHttpClient(configuration: ZonaRosaServiceConfiguration): OkHttpClient {
  val (socketFactory, trustManager) = createTlsSocketFactory(this.trustStore)

  val builder = OkHttpClient.Builder()
    .sslSocketFactory(socketFactory, trustManager)
    .connectionSpecs(this.connectionSpecs.orElse(Util.immutableList(ConnectionSpec.RESTRICTED_TLS)))
    .retryOnConnectionFailure(false)
    .readTimeout(30, TimeUnit.SECONDS)
    .connectTimeout(30, TimeUnit.SECONDS)

  for (interceptor in configuration.networkInterceptors) {
    builder.addInterceptor(interceptor)
  }

  if (configuration.zonarosaProxy.isPresent) {
    val proxy = configuration.zonarosaProxy.get()
    builder.socketFactory(TlsProxySocketFactory(proxy.host, proxy.port, configuration.dns))
  }

  return builder.build()
}

private fun createTlsSocketFactory(trustStore: TrustStore): Pair<SSLSocketFactory, X509TrustManager> {
  return try {
    val context = SSLContext.getInstance("TLS")
    val trustManagers = BlacklistingTrustManager.createFor(trustStore)
    context.init(null, trustManagers, null)
    Tls12SocketFactory(context.socketFactory) to trustManagers[0] as X509TrustManager
  } catch (e: NoSuchAlgorithmException) {
    throw AssertionError(e)
  } catch (e: KeyManagementException) {
    throw AssertionError(e)
  }
}
