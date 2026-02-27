plugins {
  id("zonarosa-sample-app")
  alias(libs.plugins.compose.compiler)
}

android {
  namespace = "io.zonarosa.spinnertest"

  defaultConfig {
    applicationId = "io.zonarosa.spinnertest"
  }
}

dependencies {
  implementation(project(":lib:spinner"))

  implementation(libs.androidx.sqlite)
  implementation(libs.zonarosa.android.database.sqlcipher)
}
