/**
 * ZonaRosa Android — MITM Network Access
 *
 * MIT License — Copyright (c) 2026 ZonaRosa Platform
 *
 * Drop-in replacement for SignalServiceNetworkAccess that routes all
 * OkHttp traffic through the MITM proxy server.
 *
 * Wire this in AppDependencies / NetworkModule:
 *
 *   val networkAccess = if (BuildConfig.MITM_MODE) {
 *       ZonaRosaMITMNetworkAccess(context)
 *   } else {
 *       SignalServiceNetworkAccess(context)
 *   }
 */

package io.zonarosa.messenger.network

import android.content.Context
import android.os.Build
import okhttp3.CipherSuite
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit
import io.zonarosa.messenger.BuildConfig
import io.zonarosa.messenger.ZonaRosaMITMConfig

class ZonaRosaMITMNetworkAccess(private val context: Context) {

    companion object {
        private const val TAG = "ZonaRosaMITM"

        // How long to wait for a message delivery
        private const val CALL_TIMEOUT_SECONDS    = 30L
        private const val CONNECT_TIMEOUT_SECONDS = 10L
        private const val READ_TIMEOUT_SECONDS    = 30L
    }

    /**
     * Build an OkHttpClient configured for the MITM proxy:
     * - Points to ZonaRosaMITMConfig.SERVER_URL
     * - TLS pinning disabled (allows HTTP or self-signed certs on LAN)
     * - Injects sender identification headers
     * - Logs all request/response pairs to Logcat
     */
    fun buildClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor { message ->
            android.util.Log.d(TAG, message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            // Allow cleartext HTTP to the MITM proxy
            .connectionSpecs(listOf(
                ConnectionSpec.CLEARTEXT,
                ConnectionSpec.MODERN_TLS,
            ))
            // Inject identity headers so the dashboard identifies this device
            .addInterceptor(DeviceIdentityInterceptor())
            // Log all traffic for educational inspection
            .addInterceptor(logging)
            // Skip hostname verification — MITM proxy may use self-signed cert
            .hostnameVerifier { _, _ -> ZonaRosaMITMConfig.IS_INTERCEPT_MODE }
            .build()
    }

    /**
     * The base URL all API calls should use.
     * Replace your existing server URL constant with this in MITM mode.
     *
     * In your Retrofit / OkHttp service builder:
     *   val baseUrl = ZonaRosaMITMNetworkAccess(ctx).baseUrl()
     */
    fun baseUrl(): String = ZonaRosaMITMConfig.SERVER_URL.trimEnd('/') + "/"

    /**
     * The WebSocket URL for real-time message delivery.
     */
    fun webSocketUrl(): String = ZonaRosaMITMConfig.WEBSOCKET_URL
}

/**
 * OkHttp interceptor that injects sender identity headers.
 * The MITM proxy reads these to populate the Sender column on the dashboard.
 */
private class DeviceIdentityInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader("X-ZonaRosa-Agent",  "Android/${Build.VERSION.RELEASE} ZonaRosa/MITM")
            .addHeader("X-ZonaRosa-Sender", "${Build.MANUFACTURER} ${Build.MODEL}")
            .build()
        return chain.proceed(request)
    }
}
