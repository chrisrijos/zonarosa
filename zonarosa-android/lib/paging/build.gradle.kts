plugins {
  id("zonarosa-library")
}

android {
  namespace = "io.zonarosa.paging"
}

dependencies {
  implementation(project(":core:util"))
}
