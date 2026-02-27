package io.zonarosa.service.api.storage

import okio.ByteString
import okio.ByteString.Companion.EMPTY
import okio.ByteString.Companion.toByteString
import io.zonarosa.core.util.ByteSize
import io.zonarosa.core.util.bytes
import io.zonarosa.core.util.isNotEmpty
import io.zonarosa.core.util.toOptional
import io.zonarosa.service.internal.storage.protos.ManifestRecord
import io.zonarosa.service.internal.storage.protos.StorageManifest
import java.util.Optional

data class ZonaRosaStorageManifest(
  @JvmField val version: Long,
  val sourceDeviceId: Int,
  val recordIkm: RecordIkm?,
  @JvmField val storageIds: List<StorageId>,
  val protoByteSize: ByteSize
) {

  constructor(version: Long, sourceDeviceId: Int, recordIkm: RecordIkm?, storageIds: List<StorageId>) : this(
    version = version,
    sourceDeviceId = sourceDeviceId,
    recordIkm = recordIkm,
    storageIds = storageIds,
    protoByteSize = toProto(version, storageIds, sourceDeviceId, recordIkm).encode().size.bytes
  )

  companion object {
    val EMPTY: ZonaRosaStorageManifest = ZonaRosaStorageManifest(0, 1, null, emptyList())

    fun deserialize(serialized: ByteArray): ZonaRosaStorageManifest {
      val manifest = StorageManifest.ADAPTER.decode(serialized)
      val manifestRecord = ManifestRecord.ADAPTER.decode(manifest.value_)
      val ids: List<StorageId> = manifestRecord.identifiers.map { id ->
        StorageId.forType(id.raw.toByteArray(), id.typeValue)
      }

      return ZonaRosaStorageManifest(
        version = manifest.version,
        sourceDeviceId = manifestRecord.sourceDevice,
        recordIkm = manifestRecord.recordIkm.takeIf { it.isNotEmpty() }?.toByteArray()?.let { RecordIkm(it) },
        storageIds = ids,
        protoByteSize = serialized.size.bytes
      )
    }

    private fun toProto(version: Long, storageIds: List<StorageId>, sourceDeviceId: Int, recordIkm: RecordIkm?): StorageManifest {
      val ids: List<ManifestRecord.Identifier> = storageIds.map { id ->
        ManifestRecord.Identifier.fromPossiblyUnknownType(id.type, id.raw)
      }

      val manifestRecord = ManifestRecord(
        identifiers = ids,
        sourceDevice = sourceDeviceId,
        recordIkm = recordIkm?.value?.toByteString() ?: ByteString.EMPTY
      )

      return StorageManifest(
        version = version,
        value_ = manifestRecord.encodeByteString()
      )
    }
  }

  val storageIdsByType: Map<Int, List<StorageId>> = storageIds.groupBy { it.type }

  val versionString: String
    get() = "$version.$sourceDeviceId"

  val accountStorageId: Optional<StorageId>
    get() = storageIdsByType[ManifestRecord.Identifier.Type.ACCOUNT.value]?.takeIf { it.isNotEmpty() }?.get(0).toOptional()

  fun serialize(): ByteArray {
    val ids: List<ManifestRecord.Identifier> = storageIds.map { id ->
      ManifestRecord.Identifier.fromPossiblyUnknownType(id.type, id.raw)
    }

    val manifestRecord = ManifestRecord(
      identifiers = ids,
      sourceDevice = sourceDeviceId,
      recordIkm = recordIkm?.value?.toByteString() ?: ByteString.EMPTY
    )

    return StorageManifest(
      version = version,
      value_ = manifestRecord.encodeByteString()
    ).encode()
  }
}
