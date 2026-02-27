pluginManagement {
  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
  }
  includeBuild("build-logic")
}
dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
    mavenLocal()
    maven {
      url = uri("https://raw.githubusercontent.com/zonarosa/maven/master/sqlcipher/release/")
      content {
        includeGroupByRegex("org\\.zonarosa.*")
      }
    }
    maven {
      url = uri("https://raw.githubusercontent.com/zonarosa/maven/master/aesgcmprovider/release/")
      content {
        includeGroupByRegex("org\\.zonarosa.*")
      }
    }
    maven {
      url = uri("https://dl.cloudsmith.io/qxAgwaeEE1vN8aLU/mobilecoin/mobilecoin/maven/")
    }
    maven {
      name = "ZonaRosaBuildArtifacts"
      url = uri("https://build-artifacts.zonarosa.io/libraries/maven/")
      content {
        includeGroupByRegex("org\\.zonarosa.*")
      }
    }
  }
  versionCatalogs {
    // libs.versions.toml is automatically registered.
    create("benchmarkLibs") {
      from(files("gradle/benchmark-libs.versions.toml"))
    }
    create("testLibs") {
      from(files("gradle/test-libs.versions.toml"))
    }
    create("lintLibs") {
      from(files("gradle/lint-libs.versions.toml"))
    }
  }
}

// To build libzonarosa from source, set the libzonarosaClientPath property in gradle.properties.
val libzonarosaClientPath = if (extra.has("libzonarosaClientPath")) extra.get("libzonarosaClientPath") else null
if (libzonarosaClientPath is String) {
  includeBuild(rootDir.resolve(libzonarosaClientPath + "/java")) {
    name = "libzonarosa-client"
    dependencySubstitution {
      substitute(module("io.zonarosa:libzonarosa-client")).using(project(":client"))
      substitute(module("io.zonarosa:libzonarosa-android")).using(project(":android"))
    }
  }
}

// Main app
include(":app")

// Core modules
include(":core:util")
include(":core:util-jvm")
include(":core:models")
include(":core:models-jvm")
include(":core:ui")

// Lib modules
include(":lib:libzonarosa-service")
include(":lib:glide")
include(":lib:photoview")
include(":lib:sticky-header-grid")
include(":lib:billing")
include(":lib:paging")
include(":lib:device-transfer")
include(":lib:donations")
include(":lib:contacts")
include(":lib:qr")
include(":lib:spinner")
include(":lib:video")
include(":lib:image-editor")
include(":lib:debuglogs-viewer")
include(":lib:blurhash")

// Feature modules
include(":feature:registration")
include(":feature:camera")
include(":feature:media-send")

// Demo apps
include(":demo:paging")
include(":demo:device-transfer")
include(":demo:donations")
include(":demo:contacts")
include(":demo:qr")
include(":demo:spinner")
include(":demo:video")
include(":demo:image-editor")
include(":demo:debuglogs-viewer")
include(":demo:registration")
include(":demo:camera")

// Testing/Lint modules
include(":lintchecks")
include(":benchmark")
include(":baseline-profile")
include(":microbenchmark")

// App project name
project(":app").name = "ZonaRosa-Android"

rootProject.name = "ZonaRosa"
