/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.core.models

import okio.ByteString
import okio.ByteString.Companion.toByteString
import io.zonarosa.core.util.UuidUtil
import io.zonarosa.core.util.toByteArray
import io.zonarosa.libzonarosa.protocol.ZonaRosaProtocolAddress
import io.zonarosa.libzonarosa.protocol.logging.Log
import java.util.UUID

/**
 * A wrapper around a UUID that represents an identifier for an account. Today, that is either an [ACI] or a [PNI].
 * However, that doesn't mean every [ServiceId] is an *instance* of one of those classes. In reality, we often
 * do not know which we have. And it shouldn't really matter.
 *
 * The only times you truly know, and the only times you should actually care, is during CDS refreshes or specific inbound messages
 * that link them together.
 */
sealed class ServiceId(val libZonaRosaServiceId: io.zonarosa.libzonarosa.protocol.ServiceId) {
  companion object {
    private const val TAG = "ServiceId"

    @JvmStatic
    fun fromLibZonaRosa(serviceId: io.zonarosa.libzonarosa.protocol.ServiceId): ServiceId {
      return when (serviceId) {
        is io.zonarosa.libzonarosa.protocol.ServiceId.Aci -> ACI(serviceId)
        is io.zonarosa.libzonarosa.protocol.ServiceId.Pni -> PNI(serviceId)
        else -> throw IllegalArgumentException("Unknown libzonarosa ServiceId type!")
      }
    }

    /** Parses a ServiceId serialized as a string. Returns null if the ServiceId is invalid. */
    @JvmOverloads
    @JvmStatic
    fun parseOrNull(raw: String?, logFailures: Boolean = true): ServiceId? {
      if (raw.isNullOrBlank()) {
        return null
      }

      return try {
        fromLibZonaRosa(io.zonarosa.libzonarosa.protocol.ServiceId.parseFromString(raw))
      } catch (e: IllegalArgumentException) {
        if (logFailures) {
          Log.w(TAG, "[parseOrNull(String)] Illegal argument!", e)
        }
        null
      } catch (e: io.zonarosa.libzonarosa.protocol.ServiceId.InvalidServiceIdException) {
        if (logFailures) {
          Log.w(TAG, "[parseOrNull(String)] Invalid ServiceId!", e)
        }
        null
      }
    }

    /** Parses a ServiceId serialized as a byte array. Returns null if the ServiceId is invalid. */
    @JvmStatic
    fun parseOrNull(raw: ByteArray?): ServiceId? {
      if (raw == null || raw.isEmpty()) {
        return null
      }

      return try {
        if (raw.size == 17) {
          fromLibZonaRosa(io.zonarosa.libzonarosa.protocol.ServiceId.parseFromFixedWidthBinary(raw))
        } else {
          fromLibZonaRosa(io.zonarosa.libzonarosa.protocol.ServiceId.parseFromBinary(raw))
        }
      } catch (e: IllegalArgumentException) {
        Log.w(TAG, "[parseOrNull(Bytes)] Illegal argument!", e)
        null
      } catch (e: io.zonarosa.libzonarosa.protocol.ServiceId.InvalidServiceIdException) {
        Log.w(TAG, "[parseOrNull(Bytes)] Invalid ServiceId!", e)
        null
      }
    }

    /** Parses a ServiceId serialized as a ByteString. Returns null if the ServiceId is invalid. */
    @JvmStatic
    fun parseOrNull(bytes: ByteString?): ServiceId? = parseOrNull(bytes?.toByteArray())

    /** Parses a ServiceId serialized as a string. Crashes if the ServiceId is invalid. */
    @JvmStatic
    @Throws(IllegalArgumentException::class)
    fun parseOrThrow(raw: String?): ServiceId = parseOrNull(raw) ?: throw IllegalArgumentException("Invalid ServiceId!")

    /** Parses a ServiceId serialized as a byte array. Crashes if the ServiceId is invalid. */
    @JvmStatic
    @Throws(IllegalArgumentException::class)
    fun parseOrThrow(raw: ByteArray): ServiceId = parseOrNull(raw) ?: throw IllegalArgumentException("Invalid ServiceId!")

    /** Parses a ServiceId serialized as a ByteString. Crashes if the ServiceId is invalid. */
    @JvmStatic
    @Throws(IllegalArgumentException::class)
    fun parseOrThrow(bytes: ByteString): ServiceId = parseOrThrow(bytes.toByteArray())

    /** Parses a ServiceId serialized as a ByteString. Returns [ACI.UNKNOWN] if not parseable. */
    @JvmStatic
    @Throws(IllegalArgumentException::class)
    fun parseOrUnknown(bytes: ByteString): ServiceId {
      return parseOrNull(bytes) ?: ACI.UNKNOWN
    }

    /** Parses a ServiceId serialized as either a byteString or string, with preference to the byteString if available. Returns null if invalid. */
    @JvmStatic
    fun parseOrNull(raw: String?, bytes: ByteString?): ServiceId? {
      return parseOrNull(bytes) ?: parseOrNull(raw)
    }

    /** Parses a ServiceId serialized as either a byteString or string, with preference to the byteString if available. Throws if invalid. */
    @JvmStatic
    @Throws(IllegalArgumentException::class)
    fun parseOrThrow(raw: String?, bytes: ByteString?): ServiceId {
      return parseOrNull(bytes) ?: parseOrThrow(raw)
    }
  }

  val rawUuid: UUID = libZonaRosaServiceId.rawUUID

  val isUnknown: Boolean = rawUuid == UuidUtil.UNKNOWN_UUID

  val isValid: Boolean = !isUnknown

  fun toProtocolAddress(deviceId: Int): ZonaRosaProtocolAddress = ZonaRosaProtocolAddress(libZonaRosaServiceId.toServiceIdString(), deviceId)

  fun toByteString(): ByteString = ByteString.Companion.of(*libZonaRosaServiceId.toServiceIdBinary())

  fun toByteArray(): ByteArray = libZonaRosaServiceId.toServiceIdBinary()

  fun logString(): String = libZonaRosaServiceId.toLogString()

  /**
   * A serialized string that can be parsed via [parseOrThrow], for instance.
   * Basically ACI's are just normal UUIDs, and PNI's are UUIDs with a `PNI:` prefix.
   */
  override fun toString(): String = libZonaRosaServiceId.toServiceIdString()

  data class ACI(val libZonaRosaAci: io.zonarosa.libzonarosa.protocol.ServiceId.Aci) : ServiceId(libZonaRosaAci) {
    companion object {
      @JvmField
      val UNKNOWN = from(UuidUtil.UNKNOWN_UUID)

      @JvmStatic
      fun from(uuid: UUID): ACI = ACI(io.zonarosa.libzonarosa.protocol.ServiceId.Aci(uuid))

      @JvmStatic
      fun fromLibZonaRosa(aci: io.zonarosa.libzonarosa.protocol.ServiceId.Aci): ACI = ACI(aci)

      @JvmStatic
      fun parseOrNull(raw: String?): ACI? = ServiceId.parseOrNull(raw).let { it as? ACI }

      @JvmStatic
      fun parseOrNull(raw: ByteArray?): ACI? = ServiceId.parseOrNull(raw).let { it as? ACI }

      @JvmStatic
      fun parseOrNull(bytes: ByteString?): ACI? = parseOrNull(bytes?.toByteArray())

      @JvmStatic
      @Throws(IllegalArgumentException::class)
      fun parseOrThrow(raw: String?): ACI = parseOrNull(raw) ?: throw IllegalArgumentException("Invalid ACI!")

      @JvmStatic
      @Throws(IllegalArgumentException::class)
      fun parseOrThrow(raw: ByteArray?): ACI = parseOrNull(raw) ?: throw IllegalArgumentException("Invalid ACI!")

      @JvmStatic
      @Throws(IllegalArgumentException::class)
      fun parseOrThrow(bytes: ByteString): ACI = parseOrThrow(bytes.toByteArray())

      @JvmStatic
      fun parseOrUnknown(bytes: ByteString?): ACI = UuidUtil.fromByteStringOrNull(bytes)?.let { from(it) } ?: UNKNOWN

      @JvmStatic
      fun parseOrUnknown(raw: String?): ACI = parseOrNull(raw) ?: UNKNOWN

      /** Parses either a byteString or string as an ACI, with preference to the byteString if available. Returns null if invalid or missing. */
      @JvmStatic
      fun parseOrNull(raw: String?, bytes: ByteString?): ACI? {
        return parseOrNull(bytes) ?: parseOrNull(raw)
      }

      /** Parses either a byteString or string as an ACI, with preference to the byteString if available. Throws if invalid or missing. */
      @JvmStatic
      @Throws(IllegalArgumentException::class)
      fun parseOrThrow(raw: String?, bytes: ByteString?): ACI {
        return parseOrNull(bytes) ?: parseOrThrow(raw)
      }
    }

    override fun toString(): String = super.toString()
  }

  data class PNI(val libZonaRosaPni: io.zonarosa.libzonarosa.protocol.ServiceId.Pni) : ServiceId(libZonaRosaPni) {
    companion object {
      @JvmField
      var UNKNOWN = from(UuidUtil.UNKNOWN_UUID)

      @JvmStatic
      fun from(uuid: UUID): PNI = PNI(io.zonarosa.libzonarosa.protocol.ServiceId.Pni(uuid))

      /** Parses a string as a PNI, regardless if the `PNI:` prefix is present or not. Only use this if you are certain that what you're reading is a PNI. */
      @JvmStatic
      fun parseOrNull(raw: String?): PNI? {
        return if (raw == null) {
          null
        } else if (raw.startsWith("PNI:")) {
          return parsePrefixedOrNull(raw)
        } else {
          val uuid = UuidUtil.parseOrNull(raw)
          if (uuid != null) {
            PNI(io.zonarosa.libzonarosa.protocol.ServiceId.Pni(uuid))
          } else {
            null
          }
        }
      }

      /** Parse a byte array as a PNI, regardless if it has the type prefix byte present or not. Only use this if you are certain what you're reading is a PNI. */
      @JvmStatic
      fun parseOrNull(raw: ByteArray?): PNI? {
        return if (raw == null || raw.isEmpty()) {
          null
        } else if (raw.size == 17) {
          ServiceId.parseOrNull(raw).let { if (it is PNI) it else null }
        } else {
          val uuid = UuidUtil.parseOrNull(raw)
          if (uuid != null) {
            PNI(io.zonarosa.libzonarosa.protocol.ServiceId.Pni(uuid))
          } else {
            null
          }
        }
      }

      /** Parses a [ByteString] as a PNI, regardless if the `PNI:` prefix is present or not. Only use this if you are certain that what you're reading is a PNI. */
      @JvmStatic
      fun parseOrNull(bytes: ByteString?): PNI? = parseOrNull(bytes?.toByteArray())

      /** Parses a string as a PNI, regardless if the `PNI:` prefix is present or not. Only use this if you are certain that what you're reading is a PNI. */
      @JvmStatic
      @Throws(IllegalArgumentException::class)
      fun parseOrThrow(raw: String?): PNI = parseOrNull(raw) ?: throw IllegalArgumentException("Invalid PNI!")

      /** Parse a byte array as a PNI, regardless if it has the type prefix byte present or not. Only use this if you are certain what you're reading is a PNI. */
      @JvmStatic
      @Throws(IllegalArgumentException::class)
      fun parseOrThrow(raw: ByteArray?): PNI = parseOrNull(raw) ?: throw IllegalArgumentException("Invalid PNI!")

      /** Parse a byte string as a PNI, regardless if it has the type prefix byte present or not. Only use this if you are certain what you're reading is a PNI. */
      @JvmStatic
      @Throws(IllegalArgumentException::class)
      fun parseOrThrow(bytes: ByteString): PNI = parseOrThrow(bytes.toByteArray())

      /** Parses a string as a PNI, expecting that the value has a `PNI:` prefix. If it does not have the prefix (or is otherwise invalid), this will return null. */
      fun parsePrefixedOrNull(raw: String?): PNI? = ServiceId.parseOrNull(raw).let { if (it is PNI) it else null }

      /** Parses either a byteString or string as a PNI, with preference to the byteString. Expecting that the value has a `PNI:` prefix. If it does not have the prefix (or is otherwise invalid), this will return null. */
      fun parsePrefixedOrNull(raw: String?, bytes: ByteString?): PNI? {
        return parseOrNull(bytes).let { if (it is PNI) it else null } ?: parsePrefixedOrNull(raw)
      }

      /** Parses either a byteString or string as a PNI, with preference to the byteString. Only use this if you are certain what you're reading is a PNI. Returns null if invalid. */
      @JvmStatic
      fun parseOrNull(raw: String?, bytes: ByteString?): PNI? {
        return parseOrNull(bytes) ?: parseOrNull(raw)
      }

      /** Parses either a byteString or string as a PNI, with preference to the byteString. Only use this if you are certain what you're reading is a PNI. Throws if missing or invalid. */
      @JvmStatic
      @Throws(IllegalArgumentException::class)
      fun parseOrThrow(raw: String?, bytes: ByteString?): PNI {
        return parseOrNull(bytes) ?: parseOrThrow(raw)
      }
    }

    override fun toString(): String = super.toString()

    /** String version without the PNI: prefix. This is only for specific proto fields. For application storage, prefer [toString]. */
    fun toStringWithoutPrefix(): String = rawUuid.toString()

    /** [ByteString] version without the PNI byte prefix. */
    fun toByteStringWithoutPrefix(): ByteString = rawUuid.toByteArray().toByteString()
  }
}
