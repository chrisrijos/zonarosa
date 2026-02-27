plugins {
  id("zonarosa-sample-app")
  alias(libs.plugins.compose.compiler)
}

android {
  namespace = "io.zonarosa.qrtest"

  defaultConfig {
    applicationId = "io.zonarosa.qrtest"
  }
}

dependencies {
  implementation(project(":lib:qr"))

  implementation(libs.google.zxing.android.integration)
  implementation(libs.google.zxing.core)
}
