package io.zonarosa.messenger.database.model

import io.zonarosa.core.util.Base64
import io.zonarosa.core.util.StringSerializer
import io.zonarosa.messenger.database.model.databaseprotos.BodyRangeList

object BodyRangeListSerializer : StringSerializer<BodyRangeList> {
  override fun serialize(data: BodyRangeList): String = Base64.encodeWithPadding(data.encode())
  override fun deserialize(data: String): BodyRangeList = BodyRangeList.ADAPTER.decode(Base64.decode(data))
}

fun BodyRangeList.serialize(): String {
  return BodyRangeListSerializer.serialize(this)
}
