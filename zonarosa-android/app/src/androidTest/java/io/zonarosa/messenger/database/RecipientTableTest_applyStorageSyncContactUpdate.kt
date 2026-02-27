/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.storage.StorageRecordUpdate
import io.zonarosa.messenger.storage.StorageSyncModels
import io.zonarosa.messenger.testing.ZonaRosaActivityRule
import io.zonarosa.messenger.util.MessageTableTestUtils
import io.zonarosa.service.api.storage.ZonaRosaContactRecord
import io.zonarosa.service.api.storage.toZonaRosaContactRecord
import io.zonarosa.service.internal.storage.protos.ContactRecord

@Suppress("ClassName")
@RunWith(AndroidJUnit4::class)
class RecipientTableTest_applyStorageSyncContactUpdate {
  @get:Rule
  val harness = ZonaRosaActivityRule()

  @Test
  fun insertMessageOnVerifiedToDefault() {
    // GIVEN
    val identities = AppDependencies.protocolStore.aci().identities()
    val other = Recipient.resolved(harness.others[0])

    MmsHelper.insert(recipient = other)
    identities.setVerified(other.id, harness.othersKeys[0].publicKey, IdentityTable.VerifiedStatus.VERIFIED)

    val oldRecord: ZonaRosaContactRecord = StorageSyncModels.localToRemoteRecord(ZonaRosaDatabase.recipients.getRecordForSync(harness.others[0])!!).let { it.proto.contact!!.toZonaRosaContactRecord(it.id) }

    val newProto = oldRecord
      .proto
      .newBuilder()
      .identityState(ContactRecord.IdentityState.DEFAULT)
      .build()
    val newRecord = ZonaRosaContactRecord(oldRecord.id, newProto)

    val update = StorageRecordUpdate<ZonaRosaContactRecord>(oldRecord, newRecord)

    // WHEN
    val oldVerifiedStatus: IdentityTable.VerifiedStatus = identities.getIdentityRecord(other.id).get().verifiedStatus
    ZonaRosaDatabase.recipients.applyStorageSyncContactUpdate(update, true)
    val newVerifiedStatus: IdentityTable.VerifiedStatus = identities.getIdentityRecord(other.id).get().verifiedStatus

    // THEN
    assertThat(oldVerifiedStatus).isEqualTo(IdentityTable.VerifiedStatus.VERIFIED)
    assertThat(newVerifiedStatus).isEqualTo(IdentityTable.VerifiedStatus.DEFAULT)

    val messages = MessageTableTestUtils.getMessages(ZonaRosaDatabase.threads.getThreadIdFor(other.id)!!)
    assertThat(messages.first().isIdentityDefault).isTrue()
  }
}
