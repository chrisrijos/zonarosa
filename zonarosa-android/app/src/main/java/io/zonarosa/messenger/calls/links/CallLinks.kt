/**
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.calls.links

import io.reactivex.rxjava3.core.Observable
import io.zonarosa.core.util.logging.Log
import io.zonarosa.ringrtc.CallException
import io.zonarosa.ringrtc.CallLinkRootKey
import io.zonarosa.messenger.database.CallLinkTable
import io.zonarosa.messenger.database.DatabaseObserver
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.service.webrtc.links.CallLinkRoomId
import java.io.UnsupportedEncodingException
import java.net.URLDecoder

/**
 * Utility object for call links to try to keep some common logic in one place.
 */
object CallLinks {
  private const val ROOT_KEY = "key"
  private const val LEGACY_HTTPS_LINK_PREFIX = "https://zonarosa.link/call#key="
  private const val LEGACY_SGNL_LINK_PREFIX = "sgnl://zonarosa.link/call#key="
  private const val HTTPS_LINK_PREFIX = "https://zonarosa.link/call/#key="
  private const val SNGL_LINK_PREFIX = "sgnl://zonarosa.link/call/#key="

  private val TAG = Log.tag(CallLinks::class.java)

  fun url(rootKeyBytes: ByteArray): String = "$HTTPS_LINK_PREFIX${CallLinkRootKey(rootKeyBytes)}"

  fun watchCallLink(roomId: CallLinkRoomId): Observable<CallLinkTable.CallLink> {
    return Observable.create { emitter ->

      fun refresh() {
        val callLink = ZonaRosaDatabase.callLinks.getCallLinkByRoomId(roomId)
        if (callLink != null) {
          emitter.onNext(callLink)
        }
      }

      val observer = DatabaseObserver.Observer {
        refresh()
      }

      AppDependencies.databaseObserver.registerCallLinkObserver(roomId, observer)
      emitter.setCancellable {
        AppDependencies.databaseObserver.unregisterObserver(observer)
      }

      refresh()
    }
  }

  private fun isPrefixedCallLink(url: String): Boolean {
    return url.startsWith(HTTPS_LINK_PREFIX) ||
      url.startsWith(SNGL_LINK_PREFIX) ||
      url.startsWith(LEGACY_HTTPS_LINK_PREFIX) ||
      url.startsWith(LEGACY_SGNL_LINK_PREFIX)
  }

  @JvmStatic
  fun isCallLink(url: String): Boolean {
    if (!isPrefixedCallLink(url)) {
      return false
    }

    return url.split("#").last().startsWith("key=")
  }

  @JvmStatic
  fun parseUrl(url: String): CallLinkRootKey? {
    if (!isPrefixedCallLink(url)) {
      Log.w(TAG, "Invalid url prefix.")
      return null
    }

    val parts = url.split("#")
    if (parts.size != 2) {
      Log.w(TAG, "Invalid fragment delimiter count in url.")
      return null
    }

    val fragmentQuery = mutableMapOf<String, String?>()

    try {
      for (part in parts[1].split("&")) {
        val kv = part.split("=")
        // Make sure we don't have an empty key (i.e. handle the case
        // of "a=0&&b=0", for example)
        if (kv[0].isEmpty()) {
          Log.w(TAG, "Invalid url: $url (empty key)")
          return null
        }
        val key = URLDecoder.decode(kv[0], "utf8")
        val value = when (kv.size) {
          1 -> null
          2 -> URLDecoder.decode(kv[1], "utf8")
          else -> {
            // Cannot have more than one value per key (i.e. handle the case
            // of "a=0&b=0=1=2", for example.
            Log.w(TAG, "Invalid url: $url (multiple values)")
            return null
          }
        }
        fragmentQuery += key to value
      }
    } catch (_: UnsupportedEncodingException) {
      Log.w(TAG, "Invalid url: $url")
      return null
    }

    val key = fragmentQuery[ROOT_KEY]
    if (key == null) {
      Log.w(TAG, "Root key not found in fragment query string.")
      return null
    }

    return try {
      return CallLinkRootKey(key)
    } catch (e: CallException) {
      Log.w(TAG, "Invalid root key found in fragment query string.")
      null
    }
  }
}
