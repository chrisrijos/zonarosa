package io.zonarosa.wire

import com.squareup.wire.schema.SchemaHandler

class Factory : SchemaHandler.Factory {
  override fun create(): SchemaHandler {
    return Handler()
  }
}
