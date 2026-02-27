package io.zonarosa.messenger.util

import android.app.Application
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.testutil.MockAppDependenciesRule
import io.zonarosa.messenger.util.ZonaRosaMeUtil.parseE164FromLink

@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(manifest = Config.NONE, application = Application::class)
class ZonaRosaMeUtilText_parseE164FromLink(private val input: String?, private val output: String?) {

  @get:Rule
  val appDependencies = MockAppDependenciesRule()

  @Before
  fun setUp() {
    mockkObject(ZonaRosaStore)
    every { ZonaRosaStore.account.e164 } returns "+15555555555"
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun parse() {
    assertEquals(output, parseE164FromLink(input))
  }

  companion object {
    @JvmStatic
    @ParameterizedRobolectricTestRunner.Parameters
    fun data(): Collection<Array<Any?>> {
      return listOf(
        arrayOf("https://zonarosa.me/#p/+15555555555", "+15555555555"),
        arrayOf("https://zonarosa.me/#p/5555555555", null),
        arrayOf("https://zonarosa.me", null),
        arrayOf("https://zonarosa.me/#p/", null),
        arrayOf("zonarosa.me/#p/+15555555555", null),
        arrayOf("sgnl://zonarosa.me/#p/+15555555555", "+15555555555"),
        arrayOf("sgnl://zonarosa.me/#p/5555555555", null),
        arrayOf("sgnl://zonarosa.me", null),
        arrayOf("sgnl://zonarosa.me/#p/", null),
        arrayOf("", null),
        arrayOf(null, null)
      )
    }
  }
}
