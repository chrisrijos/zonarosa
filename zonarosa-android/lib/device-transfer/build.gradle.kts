plugins {
  id("zonarosa-library")
}

android {
  namespace = "io.zonarosa.devicetransfer"
}

dependencies {
  implementation(project(":core:util"))
  implementation(libs.libzonarosa.android)
  api(libs.greenrobot.eventbus)

  testImplementation(testLibs.robolectric.robolectric) {
    exclude(group = "com.google.protobuf", module = "protobuf-java")
  }
  testImplementation(testFixtures(project(":lib:libzonarosa-service")))
}
