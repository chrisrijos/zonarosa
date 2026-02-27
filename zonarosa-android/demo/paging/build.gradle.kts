plugins {
  id("zonarosa-sample-app")
  alias(libs.plugins.compose.compiler)
}

android {
  namespace = "io.zonarosa.pagingtest"

  defaultConfig {
    applicationId = "io.zonarosa.pagingtest"
  }
}

dependencies {
  implementation(project(":lib:paging"))
}
