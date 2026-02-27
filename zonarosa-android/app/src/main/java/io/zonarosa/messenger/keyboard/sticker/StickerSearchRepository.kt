package io.zonarosa.messenger.keyboard.sticker

import androidx.annotation.WorkerThread
import io.zonarosa.messenger.components.emoji.EmojiUtil
import io.zonarosa.messenger.database.EmojiSearchTable
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.database.StickerTable
import io.zonarosa.messenger.database.StickerTable.StickerRecordReader
import io.zonarosa.messenger.database.model.StickerRecord

private const val RECENT_LIMIT = 24
private const val EMOJI_SEARCH_RESULTS_LIMIT = 20

class StickerSearchRepository {

  private val emojiSearchTable: EmojiSearchTable = ZonaRosaDatabase.emojiSearch
  private val stickerTable: StickerTable = ZonaRosaDatabase.stickers

  @WorkerThread
  fun search(query: String): List<StickerRecord> {
    if (query.isEmpty()) {
      return StickerRecordReader(stickerTable.getRecentlyUsedStickers(RECENT_LIMIT)).readAll()
    }

    val maybeEmojiQuery: List<StickerRecord> = findStickersForEmoji(query)
    val searchResults: List<StickerRecord> = emojiSearchTable.query(query, EMOJI_SEARCH_RESULTS_LIMIT)
      .map { findStickersForEmoji(it) }
      .flatten()

    return maybeEmojiQuery + searchResults
  }

  @WorkerThread
  private fun findStickersForEmoji(emoji: String): List<StickerRecord> {
    val searchEmoji: String = EmojiUtil.getCanonicalRepresentation(emoji)

    return EmojiUtil.getAllRepresentations(searchEmoji)
      .filterNotNull()
      .map { candidate -> StickerRecordReader(stickerTable.getStickersByEmoji(candidate)).readAll() }
      .flatten()
  }
}

private fun StickerRecordReader.readAll(): List<StickerRecord> {
  val stickers: MutableList<StickerRecord> = mutableListOf()
  use { reader ->
    var record: StickerRecord? = reader.getNext()
    while (record != null) {
      stickers.add(record)
      record = reader.getNext()
    }
  }
  return stickers
}
