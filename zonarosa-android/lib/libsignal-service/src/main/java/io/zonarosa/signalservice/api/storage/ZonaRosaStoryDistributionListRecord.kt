package io.zonarosa.service.api.storage

import io.zonarosa.service.internal.storage.protos.StoryDistributionListRecord
import java.io.IOException

data class ZonaRosaStoryDistributionListRecord(
  override val id: StorageId,
  override val proto: StoryDistributionListRecord
) : ZonaRosaRecord<StoryDistributionListRecord> {

  companion object {
    fun newBuilder(serializedUnknowns: ByteArray?): StoryDistributionListRecord.Builder {
      return serializedUnknowns?.let { builderFromUnknowns(it) } ?: StoryDistributionListRecord.Builder()
    }

    private fun builderFromUnknowns(serializedUnknowns: ByteArray): StoryDistributionListRecord.Builder {
      return try {
        StoryDistributionListRecord.ADAPTER.decode(serializedUnknowns).newBuilder()
      } catch (e: IOException) {
        StoryDistributionListRecord.Builder()
      }
    }
  }
}
