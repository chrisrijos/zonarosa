/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.database.helpers.migration

import android.app.Application
import androidx.core.content.contentValuesOf
import okio.IOException
import io.zonarosa.core.models.ServiceId
import io.zonarosa.core.util.forEach
import io.zonarosa.core.util.logging.Log
import io.zonarosa.core.util.requireBlob
import io.zonarosa.core.util.requireLong
import io.zonarosa.messenger.backup.v2.proto.GroupInvitationDeclinedUpdate
import io.zonarosa.messenger.database.SQLiteDatabase
import io.zonarosa.messenger.database.model.databaseprotos.MessageExtras

/**
 * Ensure we store ACIs only in the ACI only-field and null for PNIs for field [GroupInvitationDeclinedUpdate.inviteeAci] in [GroupInvitationDeclinedUpdate].
 */
@Suppress("ClassName")
object V267_FixGroupInvitationDeclinedUpdate : ZonaRosaDatabaseMigration {

  private val TAG = Log.tag(V267_FixGroupInvitationDeclinedUpdate::class)

  override fun migrate(context: Application, db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    val messageExtrasFixes = mutableListOf<Pair<Long, ByteArray>>()

    db.query("message", arrayOf("_id", "message_extras"), "message_extras IS NOT NULL AND type & 0x10000 != 0", null, null, null, null)
      .forEach { cursor ->
        val blob = cursor.requireBlob("message_extras")!!

        val messageExtras: MessageExtras? = try {
          MessageExtras.ADAPTER.decode(blob)
        } catch (e: IOException) {
          Log.w(TAG, "Unable to decode message extras", e)
          null
        }

        if (messageExtras?.gv2UpdateDescription?.groupChangeUpdate?.updates?.any { it.groupInvitationDeclinedUpdate != null } != true) {
          return@forEach
        }

        val groupUpdateDescription = messageExtras.gv2UpdateDescription
        val groupUpdate = groupUpdateDescription.groupChangeUpdate!!
        val updates = groupUpdate.updates.toMutableList()

        updates
          .replaceAll { change ->
            if (change.groupInvitationDeclinedUpdate != null && ServiceId.parseOrNull(change.groupInvitationDeclinedUpdate.inviteeAci) is ServiceId.PNI) {
              change.copy(groupInvitationDeclinedUpdate = change.groupInvitationDeclinedUpdate.copy(inviteeAci = null))
            } else {
              change
            }
          }

        val updatedMessageExtras = messageExtras.copy(
          gv2UpdateDescription = groupUpdateDescription.copy(
            groupChangeUpdate = groupUpdate.copy(
              updates = updates
            )
          )
        )

        messageExtrasFixes += cursor.requireLong("_id") to updatedMessageExtras.encode()
      }

    messageExtrasFixes.forEach { (id, extras) ->
      db.update("message", contentValuesOf("message_extras" to extras), "_id = $id", null)
    }
  }
}
