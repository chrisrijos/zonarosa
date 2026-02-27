# ZonaRosa MITM Intercept Dashboard

> **Educational tool** — visualizes the difference between end-to-end encrypted messaging and a Man-in-the-Middle intercept attack.

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

---

## Quick Start

```bash
cd zonarosa/mitm-dashboard
npm install
npm start          # dashboard + proxy server
node simulator.js  # optional: fake message traffic
```

Open **http://localhost:3737** in a browser.

---

## Connecting Real Mobile Clients

### 1. Find your LAN IP

The server prints it on startup:

```
  LAN IP  → http://192.168.1.42:3737
```

### 2. Android

**Build the `mitmDebug` variant:**

```bash
cd zonarosa-android

# Emulator (uses 10.0.2.2 by default):
./gradlew installMitmDebug

# Real device (same Wi-Fi network):
./gradlew installMitmDebug -Pmitm.server.url=http://192.168.1.42:3737
```

> See `mitm_build_variant.gradle.kts` for the full Gradle snippet to add to `app/build.gradle`.

**Files added:**

- `app/src/mitmDebug/java/io/zonarosa/messenger/ZonaRosaMITMConfig.kt` — server URL overrides
- `app/src/mitmDebug/res/xml/network_security_config_mitm.xml` — allows HTTP to local proxy

---

### 3. iOS

**Add the MITM scheme in Xcode:**

1. Duplicate your `Debug` scheme → rename it `ZonaRosa MITM`
2. In **Build Settings → Other Swift Flags**, add `-DMITM_MODE`
3. In **Scheme → Run → Environment Variables**, add:
   ```
   ZONAROSA_MITM_URL = http://192.168.1.42:3737
   ```
4. In your `AppEnvironment` setup, wrap the server config:
   ```swift
   #if MITM_MODE
   let serverURL = ZonaRosaMITMConfig.serverURL
   let pinning   = ZonaRosaMITMConfig.certificatePinning
   #else
   let serverURL = URL(string: TSConstants.mainServiceURL)!
   let pinning   = true
   #endif
   ```
5. Build and run on a device connected to the same Wi-Fi.

> See `ZonaRosa/ZonaRosaMITMConfig.swift` for the full configuration object.

---

## How It Works

```
iOS / Android client
       │
       │  HTTP  PUT /v1/messages/:destination
       │  WS    /v1/websocket
       ▼
┌──────────────────────────────┐
│  MITM Proxy  :3737           │
│                              │
│  MITM mode → read plaintext  │  → Dashboard shows message content
│  E2E  mode → blocked         │  → Dashboard shows ERR_DECRYPT_FAILED
└──────────────┬───────────────┘
               │  (optional) forward upstream
               ▼
       ZonaRosa Server
```

### Toggle Modes

| Mode | What the dashboard sees                                                    |
| ---- | -------------------------------------------------------------------------- |
| MITM | Sender · Recipient · Timestamp · **Plaintext content**                     |
| E2E  | Sender · Recipient · Timestamp · **`ERR_DECRYPT_FAILED`** + ciphertext hex |

Toggle in the UI or via API:

```bash
# Switch to E2E (encrypted)
curl -X POST http://localhost:3737/api/mode \
     -H 'Content-Type: application/json' \
     -d '{"newMode":"E2E"}'

# Switch back to MITM (intercept)
curl -X POST http://localhost:3737/api/mode \
     -d '{"newMode":"MITM"}'
```

### Optional: Forward to Real Server

```bash
ZONAROSA_UPSTREAM=https://chat.zonarosa.io npm start
```

---

## Protocol Endpoints Intercepted

| Method | Path                  | Purpose              |
| ------ | --------------------- | -------------------- |
| PUT    | `/v1/messages/:dest`  | Outbound messages    |
| GET    | `/v1/profile/:uuid`   | Profile lookups      |
| GET    | `/v1/keys/:uuid/:dev` | Pre-key bundle fetch |
| POST   | `/v1/accounts/*`      | Account registration |
| WS     | `/v1/websocket`       | Real-time delivery   |

---

## API Reference

| Method | Path             | Body                                      |
| ------ | ---------------- | ----------------------------------------- |
| GET    | `/api/state`     | —                                         |
| POST   | `/api/mode`      | `{ newMode: "E2E" }`                      |
| POST   | `/api/intercept` | `{ sender, recipient, plaintextContent }` |
| DELETE | `/api/messages`  | —                                         |
| GET    | `/api/lan`       | — (returns LAN IP)                        |
| WS     | `/ws/dashboard`  | Real-time message stream                  |
