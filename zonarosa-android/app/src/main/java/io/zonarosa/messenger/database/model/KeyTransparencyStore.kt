package io.zonarosa.messenger.database.model

import io.zonarosa.core.util.logging.Log.tag
import io.zonarosa.libzonarosa.keytrans.Store
import io.zonarosa.libzonarosa.protocol.ServiceId
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import java.util.Optional

/**
 * Store used by [io.zonarosa.libzonarosa.net.KeyTransparencyClient] during key transparency
 */
data object KeyTransparencyStore : Store {

  private val TAG: String = tag(KeyTransparencyStore::class.java)

  override fun getLastDistinguishedTreeHead(): Optional<ByteArray> {
    return Optional.ofNullable(ZonaRosaStore.account.distinguishedHead)
  }

  override fun setLastDistinguishedTreeHead(lastDistinguishedTreeHead: ByteArray) {
    ZonaRosaStore.account.distinguishedHead = lastDistinguishedTreeHead
  }

  override fun getAccountData(libzonarosaAci: ServiceId.Aci): Optional<ByteArray> {
    val aci = io.zonarosa.core.models.ServiceId.ACI.fromLibZonaRosa(libzonarosaAci)
    return Optional.ofNullable(ZonaRosaDatabase.recipients.getKeyTransparencyData(aci))
  }

  override fun setAccountData(libzonarosaAci: ServiceId.Aci, data: ByteArray) {
    val aci = io.zonarosa.core.models.ServiceId.ACI.fromLibZonaRosa(libzonarosaAci)
    ZonaRosaDatabase.recipients.setKeyTransparencyData(aci, data)
  }
}
