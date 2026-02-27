/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.registration.sample

import android.app.Application
import android.os.Build
import io.zonarosa.core.models.ServiceId.ACI
import io.zonarosa.core.models.ServiceId.PNI
import io.zonarosa.core.ui.CoreUiDependencies
import io.zonarosa.core.util.Base64
import io.zonarosa.core.util.logging.AndroidLogger
import io.zonarosa.core.util.logging.Log
import io.zonarosa.registration.RegistrationDependencies
import io.zonarosa.registration.sample.debug.DebugNetworkController
import io.zonarosa.registration.sample.dependencies.DemoNetworkController
import io.zonarosa.registration.sample.dependencies.DemoStorageController
import io.zonarosa.registration.sample.storage.RegistrationPreferences
import io.zonarosa.service.api.push.TrustStore
import io.zonarosa.service.api.util.CredentialsProvider
import io.zonarosa.service.internal.configuration.ZonaRosaCdnUrl
import io.zonarosa.service.internal.configuration.ZonaRosaCdsiUrl
import io.zonarosa.service.internal.configuration.ZonaRosaServiceConfiguration
import io.zonarosa.service.internal.configuration.ZonaRosaServiceUrl
import io.zonarosa.service.internal.configuration.ZonaRosaStorageUrl
import io.zonarosa.service.internal.configuration.ZonaRosaSvr2Url
import io.zonarosa.service.internal.push.PushServiceSocket
import java.io.InputStream
import java.util.Optional

class RegistrationApplication : Application() {

  companion object {
    // Staging SVR2 mrEnclave value
    private const val SVR2_MRENCLAVE = "97f151f6ed078edbbfd72fa9cae694dcc08353f1f5e8d9ccd79a971b10ffc535"
  }

  override fun onCreate() {
    super.onCreate()

    Log.initialize(AndroidLogger)

    RegistrationPreferences.init(this)

    val trustStore = SampleTrustStore()
    val configuration = createServiceConfiguration(trustStore)
    val pushServiceSocket = createPushServiceSocket(configuration)
    val demoNetworkController = DemoNetworkController(this, pushServiceSocket, configuration, SVR2_MRENCLAVE)
    val networkController = DebugNetworkController(demoNetworkController)
    val storageController = DemoStorageController(this)

    RegistrationDependencies.provide(
      RegistrationDependencies(
        networkController = networkController,
        storageController = storageController,
        sensitiveLogger = LogLogger
      )
    )

    CoreUiDependencies.init(
      this,
      object : CoreUiDependencies.Provider {
        override fun providePackageId(): String = BuildConfig.APPLICATION_ID
        override fun provideIsIncognitoKeyboardEnabled(): Boolean = false
        override fun provideIsScreenSecurityEnabled(): Boolean = false
        override fun provideForceSplitPane(): Boolean = false
      }
    )
  }

  private fun createPushServiceSocket(configuration: ZonaRosaServiceConfiguration): PushServiceSocket {
    val credentialsProvider = NoopCredentialsProvider()
    val zonarosaAgent = "ZonaRosa-Android/${BuildConfig.VERSION_NAME} Android/${Build.VERSION.SDK_INT}"

    return PushServiceSocket(
      configuration,
      credentialsProvider,
      zonarosaAgent,
      true // automaticNetworkRetry
    )
  }

  private fun createServiceConfiguration(trustStore: TrustStore): ZonaRosaServiceConfiguration {
    return ZonaRosaServiceConfiguration(
      zonarosaServiceUrls = arrayOf(ZonaRosaServiceUrl("https://chat.staging.zonarosa.io", trustStore)),
      zonarosaCdnUrlMap = mapOf(
        0 to arrayOf(ZonaRosaCdnUrl("https://cdn-staging.zonarosa.io", trustStore)),
        2 to arrayOf(ZonaRosaCdnUrl("https://cdn2-staging.zonarosa.io", trustStore)),
        3 to arrayOf(ZonaRosaCdnUrl("https://cdn3-staging.zonarosa.io", trustStore))
      ),
      zonarosaStorageUrls = arrayOf(ZonaRosaStorageUrl("https://storage-staging.zonarosa.io", trustStore)),
      zonarosaCdsiUrls = arrayOf(ZonaRosaCdsiUrl("https://cdsi.staging.zonarosa.io", trustStore)),
      zonarosaSvr2Urls = arrayOf(ZonaRosaSvr2Url("https://svr2.staging.zonarosa.io", trustStore)),
      networkInterceptors = emptyList(),
      dns = Optional.empty(),
      zonarosaProxy = Optional.empty(),
      systemHttpProxy = Optional.empty(),
      zkGroupServerPublicParams = Base64.decode("ABSY21VckQcbSXVNCGRYJcfWHiAMZmpTtTELcDmxgdFbtp/bWsSxZdMKzfCp8rvIs8ocCU3B37fT3r4Mi5qAemeGeR2X+/YmOGR5ofui7tD5mDQfstAI9i+4WpMtIe8KC3wU5w3Inq3uNWVmoGtpKndsNfwJrCg0Hd9zmObhypUnSkfYn2ooMOOnBpfdanRtrvetZUayDMSC5iSRcXKpdlukrpzzsCIvEwjwQlJYVPOQPj4V0F4UXXBdHSLK05uoPBCQG8G9rYIGedYsClJXnbrgGYG3eMTG5hnx4X4ntARBgELuMWWUEEfSK0mjXg+/2lPmWcTZWR9nkqgQQP0tbzuiPm74H2wMO4u1Wafe+UwyIlIT9L7KLS19Aw8r4sPrXZSSsOZ6s7M1+rTJN0bI5CKY2PX29y5Ok3jSWufIKcgKOnWoP67d5b2du2ZVJjpjfibNIHbT/cegy/sBLoFwtHogVYUewANUAXIaMPyCLRArsKhfJ5wBtTminG/PAvuBdJ70Z/bXVPf8TVsR292zQ65xwvWTejROW6AZX6aqucUjlENAErBme1YHmOSpU6tr6doJ66dPzVAWIanmO/5mgjNEDeK7DDqQdB1xd03HT2Qs2TxY3kCK8aAb/0iM0HQiXjxZ9HIgYhbtvGEnDKW5ILSUydqH/KBhW4Pb0jZWnqN/YgbWDKeJxnDbYcUob5ZY5Lt5ZCMKuaGUvCJRrCtuugSMaqjowCGRempsDdJEt+cMaalhZ6gczklJB/IbdwENW9KeVFPoFNFzhxWUIS5ML9riVYhAtE6JE5jX0xiHNVIIPthb458cfA8daR0nYfYAUKogQArm0iBezOO+mPk5vCNWI+wwkyFCqNDXz/qxl1gAntuCJtSfq9OC3NkdhQlgYQ=="),
      genericServerPublicParams = Base64.decode("AHILOIrFPXX9laLbalbA9+L1CXpSbM/bTJXZGZiuyK1JaI6dK5FHHWL6tWxmHKYAZTSYmElmJ5z2A5YcirjO/yfoemE03FItyaf8W1fE4p14hzb5qnrmfXUSiAIVrhaXVwIwSzH6RL/+EO8jFIjJ/YfExfJ8aBl48CKHgu1+A6kWynhttonvWWx6h7924mIzW0Czj2ROuh4LwQyZypex4GuOPW8sgIT21KNZaafgg+KbV7XM1x1tF3XA17B4uGUaDbDw2O+nR1+U5p6qHPzmJ7ggFjSN6Utu+35dS1sS0P9N"),
      backupServerPublicParams = Base64.decode("AHYrGb9IfugAAJiPKp+mdXUx+OL9zBolPYHYQz6GI1gWjpEu5me3zVNSvmYY4zWboZHif+HG1sDHSuvwFd0QszSwuSF4X4kRP3fJREdTZ5MCR0n55zUppTwfHRW2S4sdQ0JGz7YDQIJCufYSKh0pGNEHL6hv79Agrdnr4momr3oXdnkpVBIp3HWAQ6IbXQVSG18X36GaicI1vdT0UFmTwU2KTneluC2eyL9c5ff8PcmiS+YcLzh0OKYQXB5ZfQ06d6DiINvDQLy75zcfUOniLAj0lGJiHxGczin/RXisKSR8"),
      censored = false
    )
  }

  private inner class SampleTrustStore : TrustStore {
    override fun getKeyStoreInputStream(): InputStream {
      return resources.openRawResource(R.raw.zonarosa)
    }

    override fun getKeyStorePassword(): String {
      return "zonarosa"
    }
  }

  private class NoopCredentialsProvider : CredentialsProvider {
    override fun getAci(): ACI? = null
    override fun getPni(): PNI? = null
    override fun getE164(): String? = null
    override fun getDeviceId(): Int = 1
    override fun getPassword(): String? = null
  }

  private object LogLogger : Log.Logger() {
    override fun v(tag: String, message: String?, t: Throwable?, keepLonger: Boolean) {
      Log.v(tag, message, t, keepLonger)
    }

    override fun d(tag: String, message: String?, t: Throwable?, keepLonger: Boolean) {
      Log.d(tag, message, t, keepLonger)
    }

    override fun i(tag: String, message: String?, t: Throwable?, keepLonger: Boolean) {
      Log.i(tag, message, t, keepLonger)
    }

    override fun w(tag: String, message: String?, t: Throwable?, keepLonger: Boolean) {
      Log.w(tag, message, t, keepLonger)
    }

    override fun e(tag: String, message: String?, t: Throwable?, keepLonger: Boolean) {
      Log.e(tag, message, t, keepLonger)
    }

    override fun flush() {
      Log.blockUntilAllWritesFinished()
    }
  }
}
