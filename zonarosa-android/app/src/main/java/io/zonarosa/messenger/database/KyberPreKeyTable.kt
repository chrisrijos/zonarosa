package io.zonarosa.messenger.database

import android.content.Context
import io.zonarosa.core.models.ServiceId
import io.zonarosa.core.util.delete
import io.zonarosa.core.util.deleteAll
import io.zonarosa.core.util.exists
import io.zonarosa.core.util.insertInto
import io.zonarosa.core.util.logging.Log
import io.zonarosa.core.util.readToList
import io.zonarosa.core.util.readToSingleInt
import io.zonarosa.core.util.readToSingleObject
import io.zonarosa.core.util.requireBoolean
import io.zonarosa.core.util.requireNonNullBlob
import io.zonarosa.core.util.select
import io.zonarosa.core.util.toInt
import io.zonarosa.core.util.update
import io.zonarosa.core.util.withinTransaction
import io.zonarosa.libzonarosa.protocol.ecc.ECPublicKey
import io.zonarosa.libzonarosa.protocol.state.KyberPreKeyRecord

/**
 * A table for storing data related to [io.zonarosa.messenger.crypto.storage.ZonaRosaKyberPreKeyStore].
 */
class KyberPreKeyTable(context: Context, databaseHelper: ZonaRosaDatabase) : DatabaseTable(context, databaseHelper) {
  companion object {
    private val TAG = Log.tag(KyberPreKeyTable::class.java)

    const val TABLE_NAME = "kyber_prekey"
    const val ID = "_id"
    const val ACCOUNT_ID = "account_id"
    const val KEY_ID = "key_id"
    const val TIMESTAMP = "timestamp"
    const val LAST_RESORT = "last_resort"
    const val SERIALIZED = "serialized"
    const val STALE_TIMESTAMP = "stale_timestamp"

    const val CREATE_TABLE = """
      CREATE TABLE $TABLE_NAME (
        $ID INTEGER PRIMARY KEY,
        $ACCOUNT_ID TEXT NOT NULL,
        $KEY_ID INTEGER NOT NULL, 
        $TIMESTAMP INTEGER NOT NULL,
        $LAST_RESORT INTEGER NOT NULL,
        $SERIALIZED BLOB NOT NULL,
        $STALE_TIMESTAMP INTEGER NOT NULL DEFAULT 0,
        UNIQUE($ACCOUNT_ID, $KEY_ID)
    )
    """

    private const val INDEX_ACCOUNT_KEY = "kyber_account_id_key_id"

    val CREATE_INDEXES = arrayOf(
      "CREATE INDEX IF NOT EXISTS $INDEX_ACCOUNT_KEY ON $TABLE_NAME ($ACCOUNT_ID, $KEY_ID, $LAST_RESORT, $SERIALIZED)"
    )

    const val PNI_ACCOUNT_ID = "PNI"
  }

  fun get(serviceId: ServiceId, keyId: Int): KyberPreKey? {
    return readableDatabase
      .select(LAST_RESORT, SERIALIZED)
      .from("$TABLE_NAME INDEXED BY $INDEX_ACCOUNT_KEY")
      .where("$ACCOUNT_ID = ? AND $KEY_ID = ?", serviceId.toAccountId(), keyId)
      .run()
      .readToSingleObject { cursor ->
        KyberPreKey(
          record = KyberPreKeyRecord(cursor.requireNonNullBlob(SERIALIZED)),
          lastResort = cursor.requireBoolean(LAST_RESORT)
        )
      }
  }

  fun getAll(serviceId: ServiceId): List<KyberPreKey> {
    return readableDatabase
      .select(LAST_RESORT, SERIALIZED)
      .from("$TABLE_NAME INDEXED BY $INDEX_ACCOUNT_KEY")
      .where("$ACCOUNT_ID = ?", serviceId.toAccountId())
      .run()
      .readToList { cursor ->
        KyberPreKey(
          record = KyberPreKeyRecord(cursor.requireNonNullBlob(SERIALIZED)),
          lastResort = cursor.requireBoolean(LAST_RESORT)
        )
      }
  }

  fun getAllLastResort(serviceId: ServiceId): List<KyberPreKey> {
    return readableDatabase
      .select(LAST_RESORT, SERIALIZED)
      .from("$TABLE_NAME INDEXED BY $INDEX_ACCOUNT_KEY")
      .where("$ACCOUNT_ID = ? AND $LAST_RESORT = ?", serviceId.toAccountId(), 1)
      .run()
      .readToList { cursor ->
        KyberPreKey(
          record = KyberPreKeyRecord(cursor.requireNonNullBlob(SERIALIZED)),
          lastResort = cursor.requireBoolean(LAST_RESORT)
        )
      }
  }

  fun contains(serviceId: ServiceId, keyId: Int): Boolean {
    return readableDatabase
      .exists("$TABLE_NAME INDEXED BY $INDEX_ACCOUNT_KEY")
      .where("$ACCOUNT_ID = ? AND $KEY_ID = ?", serviceId.toAccountId(), keyId)
      .run()
  }

  fun insert(serviceId: ServiceId, keyId: Int, record: KyberPreKeyRecord, lastResort: Boolean) {
    writableDatabase
      .insertInto(TABLE_NAME)
      .values(
        ACCOUNT_ID to serviceId.toAccountId(),
        KEY_ID to keyId,
        TIMESTAMP to record.timestamp,
        SERIALIZED to record.serialize(),
        LAST_RESORT to lastResort.toInt()
      )
      .run(SQLiteDatabase.CONFLICT_REPLACE)
  }

  /**
   * When we mark Kyber pre-keys used, we want to keep a record of last resort tuples, which are deleted when they key
   * itself is deleted from this table via a cascading delete.
   *
   * For non-last-resort keys, this method just deletes them like normal.
   */
  fun handleMarkKyberPreKeyUsed(serviceId: ServiceId, kyberPreKeyId: Int, signedPreKeyId: Int, baseKey: ECPublicKey) {
    writableDatabase.withinTransaction { db ->
      val lastResortRowId = db
        .select(ID)
        .from(TABLE_NAME)
        .where("$ACCOUNT_ID = ? AND $KEY_ID = ? AND $LAST_RESORT = ?", serviceId.toAccountId(), kyberPreKeyId, 1)
        .run()
        .readToSingleInt(-1)

      if (lastResortRowId < 0) {
        db.delete("$TABLE_NAME INDEXED BY $INDEX_ACCOUNT_KEY")
          .where("$ACCOUNT_ID = ? AND $KEY_ID = ? AND $LAST_RESORT = ?", serviceId.toAccountId(), kyberPreKeyId, 0)
          .run()
      } else {
        ZonaRosaDatabase.lastResortKeyTuples.insert(
          kyberPreKeyRowId = lastResortRowId,
          signedKeyId = signedPreKeyId,
          publicKey = baseKey
        )
      }
    }
  }

  fun delete(serviceId: ServiceId, keyId: Int) {
    writableDatabase
      .delete("$TABLE_NAME INDEXED BY $INDEX_ACCOUNT_KEY")
      .where("$ACCOUNT_ID = ? AND $KEY_ID = ?", serviceId.toAccountId(), keyId)
      .run()
  }

  fun markAllStaleIfNecessary(serviceId: ServiceId, staleTime: Long) {
    writableDatabase
      .update(TABLE_NAME)
      .values(STALE_TIMESTAMP to staleTime)
      .where("$ACCOUNT_ID = ? AND $STALE_TIMESTAMP = 0 AND $LAST_RESORT = 0", serviceId.toAccountId())
      .run()
  }

  /**
   * Deletes all keys that have been stale since before the specified threshold.
   * We will always keep at least [minCount] items, preferring more recent ones.
   */
  fun deleteAllStaleBefore(serviceId: ServiceId, threshold: Long, minCount: Int) {
    val count = writableDatabase
      .delete(TABLE_NAME)
      .where(
        """
          $ACCOUNT_ID = ? 
            AND $LAST_RESORT = 0
            AND $STALE_TIMESTAMP > 0 
            AND $STALE_TIMESTAMP < $threshold
            AND $ID NOT IN (
              SELECT $ID
              FROM $TABLE_NAME
              WHERE 
                $ACCOUNT_ID = ?
                AND $LAST_RESORT = 0
              ORDER BY 
                CASE $STALE_TIMESTAMP WHEN 0 THEN 1 ELSE 0 END DESC,
                $STALE_TIMESTAMP DESC,
                $ID DESC
              LIMIT $minCount
            )
        """,
        serviceId.toAccountId(),
        serviceId.toAccountId()
      )
      .run()

    Log.i(TAG, "Deleted $count stale one-time EC prekeys.")
  }

  fun debugDeleteAll() {
    writableDatabase.deleteAll(OneTimePreKeyTable.TABLE_NAME)
  }

  data class KyberPreKey(
    val record: KyberPreKeyRecord,
    val lastResort: Boolean
  )

  private fun ServiceId.toAccountId(): String {
    return when (this) {
      is ServiceId.ACI -> this.toString()
      is ServiceId.PNI -> PNI_ACCOUNT_ID
    }
  }
}
