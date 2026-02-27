package io.zonarosa.messenger.migrations

import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobmanager.Job
import io.zonarosa.messenger.jobs.PaymentLedgerUpdateJob
import io.zonarosa.messenger.jobs.PaymentTransactionCheckJob

/**
 * Migration to recheck incoming payments that may have been missed due to db race.
 */
internal class RecheckPaymentsMigrationJob(
  parameters: Parameters = Parameters.Builder().build()
) : MigrationJob(parameters) {

  companion object {
    const val KEY = "RecheckPaymentsMigrationJob"

    val TAG = Log.tag(RecheckPaymentsMigrationJob::class.java)
  }

  override fun getFactoryKey(): String = KEY

  override fun isUiBlocking(): Boolean = false

  @Suppress("UsePropertyAccessSyntax")
  override fun performMigration() {
    val jobs: MutableList<Job> = ZonaRosaDatabase
      .payments
      .getSubmittedIncomingPayments()
      .filterNotNull()
      .map { PaymentTransactionCheckJob(it) }
      .toMutableList()

    Log.i(TAG, "Rechecking ${jobs.size} payments")
    if (jobs.isNotEmpty()) {
      jobs += PaymentLedgerUpdateJob.updateLedger()
    }
    AppDependencies.jobManager.addAll(jobs)
  }

  override fun shouldRetry(e: Exception): Boolean = false

  class Factory : Job.Factory<RecheckPaymentsMigrationJob> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): RecheckPaymentsMigrationJob {
      return RecheckPaymentsMigrationJob(parameters)
    }
  }
}
