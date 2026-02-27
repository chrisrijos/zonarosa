// ZonaRosa Android — MITM Build Variant Configuration
// MIT License — Copyright (c) 2026 ZonaRosa Platform
//
// This file is placed in src/mitmDebug/java/io/zonarosa/messenger/
// and overrides the production BuildConfig server URLs.
//
// To activate:
//   ./gradlew installMitmDebug
//
// The MITM_SERVER_URL is read from the `mitm.server.url` gradle property
// which you can set in local.properties:
//   mitm.server.url=http://192.168.1.42:3737
//
// Or pass at build time:
//   ./gradlew installMitmDebug -Pmitm.server.url=http://192.168.1.42:3737

package io.zonarosa.messenger

object ZonaRosaMITMConfig {

    /**
     * Set this to the LAN IP:PORT shown when you run `npm start` in
     * zonarosa/mitm-dashboard/. Example: "http://192.168.1.42:3737"
     *
     * The value can be overridden at build time via gradle property
     * `mitm.server.url` or at runtime via the BuildConfig field.
     */
    @JvmField
    val SERVER_URL: String = BuildConfig.MITM_SERVER_URL
        .takeIf { it.isNotBlank() }
        ?: "http://10.0.2.2:3737" // Android emulator → host machine

    /** WebSocket URL derived from the server URL */
    @JvmField
    val WEBSOCKET_URL: String = SERVER_URL
        .replace("http://", "ws://")
        .replace("https://", "wss://")
        .trimEnd('/') + "/v1/websocket"

    /** CDN stub — route attachments through proxy too */
    @JvmField
    val CDN_URL: String  = "$SERVER_URL/cdn"

    @JvmField
    val CDN2_URL: String = "$SERVER_URL/cdn2"

    /** Disable certificate pinning so the proxy MITM cert is accepted */
    const val CERTIFICATE_PINNING_ENABLED: Boolean = false

    const val IS_INTERCEPT_MODE: Boolean = true

    /**
     * How to wire this up in NetworkModule / AppDependencies:
     *
     * Replace:
     *   SignalServiceNetworkAccess(context)
     *
     * With:
     *   if (BuildConfig.MITM_MODE) {
     *       ZonaRosaMITMNetworkAccess(context, ZonaRosaMITMConfig.SERVER_URL)
     *   } else {
     *       SignalServiceNetworkAccess(context)
     *   }
     *
     * And in OkHttpClient builder, skip certificate pinning:
     *   if (!ZonaRosaMITMConfig.CERTIFICATE_PINNING_ENABLED) {
     *       connectionSpecs(listOf(ConnectionSpec.CLEARTEXT, ConnectionSpec.MODERN_TLS))
     *   }
     */
}
