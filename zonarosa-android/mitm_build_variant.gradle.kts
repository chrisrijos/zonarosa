/*
 * ZonaRosa Android — MITM Build Variant Gradle Snippet
 *
 * Add this to app/build.gradle inside the `android { }` block.
 * It adds a `mitmDebug` build type that:
 *   - Points the server URL at the local MITM proxy
 *   - Disables certificate pinning
 *   - Uses the MITM network security config
 *
 * Usage:
 *   # With default emulator host (10.0.2.2):
 *   ./gradlew installMitmDebug
 *
 *   # With a specific LAN IP (real device):
 *   ./gradlew installMitmDebug -Pmitm.server.url=http://192.168.1.42:3737
 */

// ── Paste inside android { buildTypes { … } } ────────────────────────────────

buildTypes {
    // ... your existing debug / release types ...

    create("mitmDebug") {
        initWith(getByName("debug"))
        isDebuggable         = true
        isMinifyEnabled      = false
        applicationIdSuffix  = ".mitm"
        versionNameSuffix    = "-MITM"
        manifestPlaceholders["networkSecurityConfig"] = "@xml/network_security_config_mitm"

        val mitmUrl = project.findProperty("mitm.server.url") as String?
            ?: "http://10.0.2.2:3737"  // default: emulator → host machine

        buildConfigField("String",  "MITM_SERVER_URL",   "\"$mitmUrl\"")
        buildConfigField("Boolean", "MITM_MODE",         "true")
        buildConfigField("Boolean", "CERTIFICATE_PINNING_ENABLED", "false")

        resValue("string", "zonarosa_server_url", mitmUrl)
    }
}

// ── Also add in defaultConfig { } ────────────────────────────────────────────
// buildConfigField("String",  "MITM_SERVER_URL",   "\"\"")
// buildConfigField("Boolean", "MITM_MODE",         "false")
// buildConfigField("Boolean", "CERTIFICATE_PINNING_ENABLED", "true")
