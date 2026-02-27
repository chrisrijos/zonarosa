package io.zonarosa.lint

import com.android.tools.lint.checks.infrastructure.TestFiles.java
import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask
import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Scanner

class ZonaRosaLogDetectorTest {
  @Test
  fun androidLogUsed_LogNotZonaRosa_2_args() {
    TestLintTask.lint()
      .files(
        androidLogStub,
        java(
          """
          package foo;
          import android.util.Log;
          public class Example {
            public void log() {
              Log.d("TAG", "msg");
            }
          }
          """.trimIndent()
        )
      )
      .issues(ZonaRosaLogDetector.LOG_NOT_ZONAROSA)
      .allowMissingSdk()
      .run()
      .expect(
        """
        src/foo/Example.java:5: Error: Using 'android.util.Log' instead of a ZonaRosa Logger [LogNotZonaRosa]
            Log.d("TAG", "msg");
            ~~~~~~~~~~~~~~~~~~~
        1 errors, 0 warnings
        """.trimIndent()
      )
      .expectFixDiffs(
        """
        Fix for src/foo/Example.java line 5: Replace with io.zonarosa.core.util.logging.Log.d("TAG", "msg"):
        @@ -5 +5
        -     Log.d("TAG", "msg");
        +     io.zonarosa.core.util.logging.Log.d("TAG", "msg");
        """.trimIndent()
      )
  }


  @Test
  fun zonarosaServiceLogUsed_LogNotApp_2_args() {
    TestLintTask.lint()
      .files(
        serviceLogStub,
        java(
          """
          package foo;
          import io.zonarosa.libzonarosa.protocol.logging.Log;
          public class Example {
            public void log() {
              Log.d("TAG", "msg");
            }
          }
          """.trimIndent()
        )
      )
      .issues(ZonaRosaLogDetector.LOG_NOT_APP)
      .allowMissingSdk()
      .run()
      .expect(
        """
        src/foo/Example.java:5: Error: Using ZonaRosa server logger instead of app level Logger [LogNotAppZonaRosa]
            Log.d("TAG", "msg");
            ~~~~~~~~~~~~~~~~~~~
        1 errors, 0 warnings
        """.trimIndent()
      )
      .expectFixDiffs(
        """
        Fix for src/foo/Example.java line 5: Replace with io.zonarosa.core.util.logging.Log.d("TAG", "msg"):
        @@ -5 +5
        -     Log.d("TAG", "msg");
        +     io.zonarosa.core.util.logging.Log.d("TAG", "msg");
        """.trimIndent()
      )
  }

  @Test
  fun zonarosaServiceLogUsed_LogNotApp_3_args() {
    TestLintTask.lint()
      .files(
        serviceLogStub,
        java(
          """
          package foo;
          import io.zonarosa.libzonarosa.protocol.logging.Log;
          public class Example {
            public void log() {
              Log.w("TAG", "msg", new Exception());
            }
          }
          """.trimIndent()
        )
      )
      .issues(ZonaRosaLogDetector.LOG_NOT_APP)
      .allowMissingSdk()
      .run()
      .expectContains("""
        src/foo/Example.java:5: Error: Using ZonaRosa server logger instead of app level Logger [LogNotAppZonaRosa]
        """.trimIndent()
      )
  }

  @Test
  fun log_uses_tag_constant() {
    TestLintTask.lint()
      .files(
        appLogStub,
        java(
          """
          package foo;
          import io.zonarosa.core.util.logging.Log;
          public class Example {
            private static final String TAG = Log.tag(Example.class);
            public void log() {
              Log.d(TAG, "msg");
            }
          }
          """.trimIndent()
        )
      )
      .issues(ZonaRosaLogDetector.INLINE_TAG)
      .allowMissingSdk()
      .skipTestModes(TestMode.FULLY_QUALIFIED)
      .run()
      .expectClean()
  }

  @Test
  fun log_uses_tag_constant_kotlin() {
    TestLintTask.lint()
      .files(
        appLogStub,
        kotlin(
          """
          package foo
          import io.zonarosa.core.util.logging.Log
          class Example {
            const val TAG: String = Log.tag(Example::class.java)
            fun log() {
              Log.d(TAG, "msg")
            }
          }
          """.trimIndent()
        )
      )
      .issues(ZonaRosaLogDetector.INLINE_TAG)
      .allowMissingSdk()
      .skipTestModes(TestMode.REORDER_ARGUMENTS)
      .run()
      .expectClean()
  }

  @Test
  fun log_uses_tag_companion_kotlin() {
    TestLintTask.lint()
      .files(
        appLogStub,
        kotlin(
          """
          package foo
          import io.zonarosa.core.util.logging.Log
          class Example {
            companion object { val TAG: String = Log.tag(Example::class.java) }
            fun log() {
              Log.d(TAG, "msg")
            }
          }
          fun logOutsie() {
            Log.d(Example.TAG, "msg")
          }
          """.trimIndent()
        )
      )
      .issues(ZonaRosaLogDetector.INLINE_TAG)
      .allowMissingSdk()
      .skipTestModes(TestMode.REORDER_ARGUMENTS)
      .run()
      .expectClean()
  }

  @Test
  fun log_uses_inline_tag() {
    TestLintTask.lint()
      .files(
        appLogStub,
        java(
          """
          package foo;
          import io.zonarosa.core.util.logging.Log;
          public class Example {
            public void log() {
              Log.d("TAG", "msg");
            }
          }
          """.trimIndent()
        )
      )
      .issues(ZonaRosaLogDetector.INLINE_TAG)
      .allowMissingSdk()
      .run()
      .expect(
        """
        src/foo/Example.java:5: Error: Not using a tag constant [LogTagInlined]
            Log.d("TAG", "msg");
            ~~~~~~~~~~~~~~~~~~~
        1 errors, 0 warnings
        """.trimIndent()
      )
      .expectFixDiffs("")
  }

  @Test
  fun log_uses_inline_tag_kotlin() {
    TestLintTask.lint()
      .files(
        appLogStub,
        kotlin(
          """
          package foo
          import io.zonarosa.core.util.logging.Log
          class Example {
            fun log() {
              Log.d("TAG", "msg")
            }
          }
          """.trimIndent()
        )
      )
      .issues(ZonaRosaLogDetector.INLINE_TAG)
      .allowMissingSdk()
      .run()
      .expect(
        """
        src/foo/Example.kt:5: Error: Not using a tag constant [LogTagInlined]
            Log.d("TAG", "msg")
            ~~~~~~~~~~~~~~~~~~~
        1 errors, 0 warnings
        """.trimIndent()
      )
      .expectFixDiffs("")
  }

  @Test
  fun glideLogUsed_LogNotZonaRosa_2_args() {
    TestLintTask.lint()
      .files(
        glideLogStub,
        java(
          """
          package foo;
          import io.zonarosa.glide.Log;
          public class Example {
            public void log() {
              Log.d("TAG", "msg");
            }
          }
          """.trimIndent()
        )
      )
      .issues(ZonaRosaLogDetector.LOG_NOT_ZONAROSA)
      .allowMissingSdk()
      .run()
      .expect(
        """
        src/foo/Example.java:5: Error: Using 'io.zonarosa.glide.Log' instead of a ZonaRosa Logger [LogNotZonaRosa]
            Log.d("TAG", "msg");
            ~~~~~~~~~~~~~~~~~~~
        1 errors, 0 warnings
        """.trimIndent()
      )
      .expectFixDiffs(
        """
        Fix for src/foo/Example.java line 5: Replace with io.zonarosa.core.util.logging.Log.d("TAG", "msg"):
        @@ -5 +5
        -     Log.d("TAG", "msg");
        +     io.zonarosa.core.util.logging.Log.d("TAG", "msg");
        """.trimIndent()
      )
  }

  companion object {
    private val androidLogStub = kotlin(readResourceAsString("AndroidLogStub.kt"))
    private val serviceLogStub = kotlin(readResourceAsString("ServiceLogStub.kt"))
    private val appLogStub = kotlin(readResourceAsString("AppLogStub.kt"))
    private val glideLogStub = kotlin(readResourceAsString("GlideLogStub.kt"))

    private fun readResourceAsString(resourceName: String): String {
      val inputStream = ClassLoader.getSystemClassLoader().getResourceAsStream(resourceName)
      assertNotNull(inputStream)
      val scanner = Scanner(inputStream!!).useDelimiter("\\A")
      assertTrue(scanner.hasNext())
      return scanner.next()
    }
  }
}
