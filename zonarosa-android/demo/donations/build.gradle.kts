plugins {
  id("zonarosa-sample-app")
  alias(libs.plugins.compose.compiler)
}

android {
  namespace = "io.zonarosa.donations.app"

  defaultConfig {
    applicationId = "io.zonarosa.donations.app"
  }
}

dependencies {
  implementation(project(":lib:donations"))
  implementation(project(":core:util"))
}
