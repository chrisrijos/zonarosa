package io.zonarosa.messenger.testing

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import io.zonarosa.messenger.ZonaRosaInstrumentationApplicationContext

/**
 * Custom runner that replaces application with [ZonaRosaInstrumentationApplicationContext].
 */
@Suppress("unused")
class ZonaRosaTestRunner : AndroidJUnitRunner() {
  override fun newApplication(cl: ClassLoader?, className: String?, context: Context?): Application {
    return super.newApplication(cl, ZonaRosaInstrumentationApplicationContext::class.java.name, context)
  }
}
