/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.util

import org.junit.Assert.assertEquals
import io.zonarosa.core.models.ServiceId
import io.zonarosa.core.models.ServiceId.ACI
import io.zonarosa.core.models.ServiceId.PNI
import io.zonarosa.core.util.readToSingleObject
import io.zonarosa.core.util.requireLongOrNull
import io.zonarosa.core.util.select
import io.zonarosa.core.util.update
import io.zonarosa.libzonarosa.protocol.ecc.ECKeyPair
import io.zonarosa.libzonarosa.protocol.ecc.ECPublicKey
import io.zonarosa.libzonarosa.protocol.kem.KEMKeyPair
import io.zonarosa.libzonarosa.protocol.kem.KEMKeyType
import io.zonarosa.libzonarosa.protocol.state.KyberPreKeyRecord
import io.zonarosa.messenger.database.KyberPreKeyTable
import io.zonarosa.messenger.database.ZonaRosaDatabase
import java.security.SecureRandom

object KyberPreKeysTestUtil {
  fun insertTestRecord(account: ServiceId, id: Int, staleTime: Long = 0, lastResort: Boolean = false) {
    val kemKeyPair = KEMKeyPair.generate(KEMKeyType.KYBER_1024)
    ZonaRosaDatabase.kyberPreKeys.insert(
      serviceId = account,
      keyId = id,
      record = KyberPreKeyRecord(
        id,
        System.currentTimeMillis(),
        kemKeyPair,
        ECKeyPair.generate().privateKey.calculateSignature(kemKeyPair.publicKey.serialize())
      ),
      lastResort = lastResort
    )

    val count = ZonaRosaDatabase.rawDatabase
      .update(KyberPreKeyTable.TABLE_NAME)
      .values(KyberPreKeyTable.STALE_TIMESTAMP to staleTime)
      .where("${KyberPreKeyTable.ACCOUNT_ID} = ? AND ${KyberPreKeyTable.KEY_ID} = $id", account.toAccountId())
      .run()

    assertEquals(1, count)
  }

  fun getStaleTime(account: ServiceId, id: Int): Long? {
    return ZonaRosaDatabase.rawDatabase
      .select(KyberPreKeyTable.STALE_TIMESTAMP)
      .from(KyberPreKeyTable.TABLE_NAME)
      .where("${KyberPreKeyTable.ACCOUNT_ID} = ? AND ${KyberPreKeyTable.KEY_ID} = $id", account.toAccountId())
      .run()
      .readToSingleObject { it.requireLongOrNull(KyberPreKeyTable.STALE_TIMESTAMP) }
  }

  fun generateECPublicKey(): ECPublicKey {
    val byteArray = ByteArray(ECPublicKey.KEY_SIZE - 1)
    SecureRandom().nextBytes(byteArray)

    return ECPublicKey.fromPublicKeyBytes(byteArray)
  }

  private fun ServiceId.toAccountId(): String {
    return when (this) {
      is ACI -> this.toString()
      is PNI -> KyberPreKeyTable.PNI_ACCOUNT_ID
    }
  }
}
