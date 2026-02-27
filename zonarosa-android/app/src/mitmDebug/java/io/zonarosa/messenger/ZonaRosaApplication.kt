/**
 * ZonaRosa Android — MITM Application Entry Point
 *
 * MIT License — Copyright (c) 2026 ZonaRosa Platform
 *
 * Application subclass for the mitmDebug build variant.
 * Placed in src/mitmDebug/ so it is only compiled into MITM builds.
 *
 * This overrides the production AppDependencies to swap in the
 * MITM network client before any service connections are made.
 *
 * Android resolves the correct Application class per build variant:
 *   src/main/        → ZonaRosaApplication (production)
 *   src/mitmDebug/   → ZonaRosaMITMApplication (MITM lab)
 */

package io.zonarosa.messenger

import android.app.Application
import android.util.Log
import io.zonarosa.messenger.network.ZonaRosaMITMNetworkAccess

class ZonaRosaApplication : Application() {

    companion object {
        private const val TAG = "ZonaRosaMITM"

        // Accessed by NetworkModule / Retrofit builders
        lateinit var mitmNetworkAccess: ZonaRosaMITMNetworkAccess
            private set
    }

    override fun onCreate() {
        super.onCreate()

        Log.w(TAG, "═══════════════════════════════════════════════")
        Log.w(TAG, "  ZonaRosa MITM MODE ACTIVE")
        Log.w(TAG, "  Server: ${ZonaRosaMITMConfig.SERVER_URL}")
        Log.w(TAG, "  All messages visible to the intercept dashboard")
        Log.w(TAG, "═══════════════════════════════════════════════")

        mitmNetworkAccess = ZonaRosaMITMNetworkAccess(this)

        // Override the global base URL before any service is initialised.
        // In the production codebase, replace wherever TextSecureServerConstants
        // or BuildConfig.SERVER_URL is consumed with:
        //
        //   if (BuildConfig.MITM_MODE) ZonaRosaApplication.mitmNetworkAccess.baseUrl()
        //   else BuildConfig.SERVER_URL
        //
        System.setProperty("zonarosa.server.url",   ZonaRosaMITMConfig.SERVER_URL)
        System.setProperty("zonarosa.websocket.url", ZonaRosaMITMConfig.WEBSOCKET_URL)
    }
}
