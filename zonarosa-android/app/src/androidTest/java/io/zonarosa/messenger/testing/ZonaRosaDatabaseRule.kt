package io.zonarosa.messenger.testing

import org.junit.rules.TestWatcher
import org.junit.runner.Description
import io.zonarosa.core.models.ServiceId.ACI
import io.zonarosa.core.models.ServiceId.PNI
import io.zonarosa.core.util.deleteAll
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.database.ThreadTable
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import java.util.UUID

/**
 * Sets up bare-minimum to allow writing unit tests against the database,
 * including setting up the local ACI and PNI pair.
 *
 * @param deleteAllThreadsOnEachRun Run deleteAllThreads between each unit test
 */
class ZonaRosaDatabaseRule(
  private val deleteAllThreadsOnEachRun: Boolean = true
) : TestWatcher() {

  val localAci: ACI = ACI.from(UUID.randomUUID())
  val localPni: PNI = PNI.from(UUID.randomUUID())

  override fun starting(description: Description?) {
    deleteAllThreads()

    ZonaRosaStore.account.setAci(localAci)
    ZonaRosaStore.account.setPni(localPni)
  }

  override fun finished(description: Description?) {
    deleteAllThreads()
  }

  private fun deleteAllThreads() {
    if (deleteAllThreadsOnEachRun) {
      ZonaRosaDatabase.threads.deleteAllConversations()
      ZonaRosaDatabase.rawDatabase.deleteAll(ThreadTable.TABLE_NAME)
    }
  }
}
