package io.zonarosa.messenger

import android.content.ContentValues
import android.os.Build
import io.zonarosa.core.util.logging.AndroidLogger
import io.zonarosa.core.util.logging.Log
import io.zonarosa.spinner.Spinner
import io.zonarosa.spinner.Spinner.DatabaseConfig
import io.zonarosa.spinner.SpinnerLogger
import io.zonarosa.messenger.database.AttachmentTransformer
import io.zonarosa.messenger.database.DatabaseMonitor
import io.zonarosa.messenger.database.GV2Transformer
import io.zonarosa.messenger.database.GV2UpdateTransformer
import io.zonarosa.messenger.database.IdPopupTransformer
import io.zonarosa.messenger.database.IsStoryTransformer
import io.zonarosa.messenger.database.JobDatabase
import io.zonarosa.messenger.database.KeyValueDatabase
import io.zonarosa.messenger.database.KyberKeyTransformer
import io.zonarosa.messenger.database.LocalMetricsDatabase
import io.zonarosa.messenger.database.LogDatabase
import io.zonarosa.messenger.database.MegaphoneDatabase
import io.zonarosa.messenger.database.MessageBitmaskColumnTransformer
import io.zonarosa.messenger.database.MessageRangesTransformer
import io.zonarosa.messenger.database.PollTransformer
import io.zonarosa.messenger.database.ProfileKeyCredentialTransformer
import io.zonarosa.messenger.database.QueryMonitor
import io.zonarosa.messenger.database.RecipientTransformer
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.database.ZonaRosaStoreTransformer
import io.zonarosa.messenger.database.TimestampTransformer
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.logging.PersistentLogger
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.util.AppSignatureUtil
import io.zonarosa.messenger.util.RemoteConfig
import java.util.Locale

class SpinnerApplicationContext : ApplicationContext() {
  override fun onCreate() {
    super.onCreate()

    try {
      Class.forName("dalvik.system.CloseGuard")
        .getMethod("setEnabled", Boolean::class.javaPrimitiveType)
        .invoke(null, true)
    } catch (e: ReflectiveOperationException) {
      throw RuntimeException(e)
    }

    Spinner.init(
      this,
      mapOf(
        "Device" to { "${Build.MODEL} (Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT})" },
        "Package" to { "$packageName (${AppSignatureUtil.getAppSignature(this)})" },
        "App Version" to { "${BuildConfig.VERSION_NAME} (${BuildConfig.CANONICAL_VERSION_CODE}, ${BuildConfig.GIT_HASH})" },
        "Profile Name" to { (if (ZonaRosaStore.account.isRegistered) Recipient.self().profileName.toString() else "none") },
        "E164" to { ZonaRosaStore.account.e164 ?: "none" },
        "ACI" to { ZonaRosaStore.account.aci?.toString() ?: "none" },
        "PNI" to { ZonaRosaStore.account.pni?.toString() ?: "none" },
        Spinner.KEY_ENVIRONMENT to { BuildConfig.FLAVOR_environment.uppercase(Locale.US) }
      ),
      linkedMapOf(
        "zonarosa" to DatabaseConfig(
          db = { ZonaRosaDatabase.rawDatabase },
          columnTransformers = listOf(
            MessageBitmaskColumnTransformer,
            GV2Transformer,
            GV2UpdateTransformer,
            IsStoryTransformer,
            TimestampTransformer,
            ProfileKeyCredentialTransformer,
            MessageRangesTransformer,
            KyberKeyTransformer,
            RecipientTransformer,
            AttachmentTransformer,
            PollTransformer,
            IdPopupTransformer
          )
        ),
        "jobmanager" to DatabaseConfig(db = { JobDatabase.getInstance(this).sqlCipherDatabase }, columnTransformers = listOf(TimestampTransformer)),
        "keyvalue" to DatabaseConfig(db = { KeyValueDatabase.getInstance(this).sqlCipherDatabase }, columnTransformers = listOf(ZonaRosaStoreTransformer)),
        "megaphones" to DatabaseConfig(db = { MegaphoneDatabase.getInstance(this).sqlCipherDatabase }),
        "localmetrics" to DatabaseConfig(db = { LocalMetricsDatabase.getInstance(this).sqlCipherDatabase }),
        "logs" to DatabaseConfig(
          db = { LogDatabase.getInstance(this).sqlCipherDatabase },
          columnTransformers = listOf(TimestampTransformer)
        )
      ),
      linkedMapOf(
        StorageServicePlugin.PATH to StorageServicePlugin(),
        AttachmentPlugin.PATH to AttachmentPlugin(),
        BackupPlugin.PATH to BackupPlugin(),
        ApiPlugin.PATH to ApiPlugin()
      )
    )

    Log.initialize({ RemoteConfig.internalUser }, AndroidLogger, PersistentLogger.getInstance(this), SpinnerLogger)

    DatabaseMonitor.initialize(object : QueryMonitor {
      override fun onSql(sql: String, args: Array<Any>?) {
        Spinner.onSql("zonarosa", sql, args)
      }

      override fun onQuery(distinct: Boolean, table: String, projection: Array<String>?, selection: String?, args: Array<Any>?, groupBy: String?, having: String?, orderBy: String?, limit: String?) {
        Spinner.onQuery("zonarosa", distinct, table, projection, selection, args, groupBy, having, orderBy, limit)
      }

      override fun onDelete(table: String, selection: String?, args: Array<Any>?) {
        Spinner.onDelete("zonarosa", table, selection, args)
      }

      override fun onUpdate(table: String, values: ContentValues, selection: String?, args: Array<Any>?) {
        Spinner.onUpdate("zonarosa", table, values, selection, args)
      }
    })
  }
}
