package io.zonarosa.messenger.net

import okhttp3.Interceptor
import okhttp3.Response
import io.zonarosa.core.util.logging.Log
import io.zonarosa.core.util.logging.Log.tag
import io.zonarosa.core.util.orNull
import io.zonarosa.messenger.keyvalue.ZonaRosaStore.Companion.misc
import io.zonarosa.service.internal.configuration.ZonaRosaServiceConfiguration
import java.io.IOException

/**
 * Marks the client as remotely-deprecated when it receives a 499 response.
 */
class RemoteDeprecationDetectorInterceptor(private val getConfiguration: () -> ZonaRosaServiceConfiguration) : Interceptor {

  companion object {
    private val TAG = tag(RemoteDeprecationDetectorInterceptor::class.java)
  }

  @Throws(IOException::class)
  override fun intercept(chain: Interceptor.Chain): Response {
    val request = chain.request()
    val response = chain.proceed(request)

    if (response.code == 499 && !misc.isClientDeprecated && getConfiguration().zonarosaServiceUrls.any { request.url.toString().startsWith(it.url) && it.hostHeader.orNull() == request.header("host") }) {
      Log.w(TAG, "Received 499. Client version is deprecated.", true)
      misc.isClientDeprecated = true
    }

    return response
  }
}
