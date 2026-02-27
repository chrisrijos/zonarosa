package io.zonarosa.messenger.components.settings.app.usernamelinks.colorpicker

import kotlinx.collections.immutable.ImmutableList
import io.zonarosa.messenger.components.settings.app.usernamelinks.QrCodeState
import io.zonarosa.messenger.components.settings.app.usernamelinks.UsernameQrCodeColorScheme

data class UsernameLinkQrColorPickerState(
  val username: String,
  val qrCodeData: QrCodeState,
  val colorSchemes: ImmutableList<UsernameQrCodeColorScheme>,
  val selectedColorScheme: UsernameQrCodeColorScheme
)
