plugins {
  id("zonarosa-library")
  id("com.squareup.wire")
  id("kotlin-parcelize")
  alias(libs.plugins.kotlinx.serialization)
}

android {
  namespace = "io.zonarosa.core.util"
}

dependencies {
  api(project(":core:util-jvm"))

  implementation(libs.androidx.sqlite)
  implementation(libs.androidx.documentfile)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.jackson.core)
  implementation(libs.google.libphonenumber)
  testImplementation(libs.androidx.sqlite.framework)

  testImplementation(testLibs.junit.junit)
  testImplementation(testLibs.assertk)
  testImplementation(testLibs.robolectric.robolectric)
}

wire {
  kotlin {
    javaInterop = true
  }

  sourcePath {
    srcDir("src/main/protowire")
  }
}
