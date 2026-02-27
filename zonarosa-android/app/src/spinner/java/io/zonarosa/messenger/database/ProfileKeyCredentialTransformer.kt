package io.zonarosa.messenger.database

import android.database.Cursor
import io.zonarosa.core.util.Base64
import io.zonarosa.core.util.Hex
import io.zonarosa.core.util.requireString
import io.zonarosa.libzonarosa.zkgroup.profiles.ExpiringProfileKeyCredential
import io.zonarosa.spinner.ColumnTransformer
import io.zonarosa.spinner.DefaultColumnTransformer
import io.zonarosa.messenger.database.model.databaseprotos.ExpiringProfileKeyCredentialColumnData
import io.zonarosa.messenger.util.toLocalDateTime
import java.security.MessageDigest

object ProfileKeyCredentialTransformer : ColumnTransformer {
  override fun matches(tableName: String?, columnName: String): Boolean {
    return columnName == RecipientTable.EXPIRING_PROFILE_KEY_CREDENTIAL && (tableName == null || tableName == RecipientTable.TABLE_NAME)
  }

  override fun transform(tableName: String?, columnName: String, cursor: Cursor): String? {
    val columnDataString = cursor.requireString(RecipientTable.EXPIRING_PROFILE_KEY_CREDENTIAL) ?: return DefaultColumnTransformer.transform(tableName, columnName, cursor)
    val columnDataBytes = Base64.decode(columnDataString)
    val columnData = ExpiringProfileKeyCredentialColumnData.ADAPTER.decode(columnDataBytes)
    val credential = ExpiringProfileKeyCredential(columnData.expiringProfileKeyCredential.toByteArray())

    return """
      Credential: ${Hex.toStringCondensed(MessageDigest.getInstance("SHA-256").digest(credential.serialize()))}
      Expires:    ${credential.expirationTime.toLocalDateTime()}
      
      Matching Profile Key: 
        ${Base64.encodeWithPadding(columnData.profileKey.toByteArray())}
    """.trimIndent().replace("\n", "<br>")
  }
}
