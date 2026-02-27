plugins {
  id("zonarosa-library")
}

android {
  namespace = "io.zonarosa.video"
}

dependencies {
  implementation(project(":core:util"))
  implementation(libs.libzonarosa.android)
  implementation(libs.google.guava.android)

  implementation(libs.bundles.mp4parser) {
    exclude(group = "junit", module = "junit")
  }
}
