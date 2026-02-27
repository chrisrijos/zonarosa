plugins {
  id("zonarosa-sample-app")
  alias(libs.plugins.compose.compiler)
}

android {
  namespace = "io.zonarosa.debuglogsviewer.app"

  defaultConfig {
    applicationId = "io.zonarosa.debuglogsviewer.app"
  }
}

dependencies {
  implementation(project(":lib:debuglogs-viewer"))
  implementation(project(":core:util"))
}
