package io.zonarosa.messenger.database

import android.database.Cursor
import io.zonarosa.core.util.requireInt
import io.zonarosa.spinner.ColumnTransformer
import io.zonarosa.messenger.database.model.StoryType.Companion.fromCode

object IsStoryTransformer : ColumnTransformer {
  override fun matches(tableName: String?, columnName: String): Boolean {
    return columnName == MessageTable.STORY_TYPE && (tableName == null || tableName == MessageTable.TABLE_NAME)
  }

  override fun transform(tableName: String?, columnName: String, cursor: Cursor): String? {
    val storyType = fromCode(cursor.requireInt(MessageTable.STORY_TYPE))
    return "${cursor.requireInt(MessageTable.STORY_TYPE)}<br><br>$storyType"
  }
}
