package io.zonarosa.messenger

import io.zonarosa.core.util.concurrent.ZonaRosaExecutors
import io.zonarosa.core.util.logging.AndroidLogger
import io.zonarosa.core.util.logging.Log
import io.zonarosa.libzonarosa.protocol.logging.ZonaRosaProtocolLoggerProvider
import io.zonarosa.messenger.database.LogDatabase
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.dependencies.ApplicationDependencyProvider
import io.zonarosa.messenger.dependencies.InstrumentationApplicationDependencyProvider
import io.zonarosa.messenger.logging.CustomZonaRosaProtocolLogger
import io.zonarosa.messenger.logging.PersistentLogger
import io.zonarosa.messenger.testing.InMemoryLogger

/**
 * Application context for running instrumentation tests (aka androidTests).
 */
class ZonaRosaInstrumentationApplicationContext : ApplicationContext() {

  val inMemoryLogger: InMemoryLogger = InMemoryLogger()

  override fun initializeAppDependencies() {
    val default = ApplicationDependencyProvider(this)
    AppDependencies.init(this, InstrumentationApplicationDependencyProvider(this, default))
    AppDependencies.deadlockDetector.start()
  }

  override fun initializeLogging() {
    Log.initialize({ true }, AndroidLogger, PersistentLogger.getInstance(this), inMemoryLogger)

    ZonaRosaProtocolLoggerProvider.setProvider(CustomZonaRosaProtocolLogger())

    ZonaRosaExecutors.UNBOUNDED.execute {
      Log.blockUntilAllWritesFinished()
      LogDatabase.getInstance(this).logs.trimToSize()
    }
  }

  override fun beginJobLoop() = Unit

  /**
   * Some of the jobs can interfere with some of the instrumentation tests.
   *
   * For example, we may try to create a release channel recipient while doing
   * an import/backup test.
   *
   * This can be used to start the job loop if needed for tests that rely on it.
   */
  fun beginJobLoopForTests() {
    super.beginJobLoop()
  }
}
