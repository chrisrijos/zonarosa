/**
 * ZonaRosa iOS — MITM Server Configuration
 *
 * MIT License — Copyright (c) 2026 ZonaRosa Platform
 *
 * Swap this in for your normal TextSecureServerConstants.swift to route
 * all ZonaRosa protocol traffic through the MITM intercept proxy.
 *
 * Usage:
 *   1. Start the MITM dashboard:  cd mitm-dashboard && npm start
 *   2. Note the LAN IP printed on startup, e.g. http://192.168.1.42:3737
 *   3. Set MITM_SERVER_URL below to that address
 *   4. Build and run the app on a device connected to the same Wi-Fi network
 *   5. All messages will appear on the dashboard at http://localhost:3737
 */

import Foundation

// MARK: - MITM Server URL
// ⚠️ Set this to the IP shown in `npm start` output, e.g. "http://192.168.1.42:3737"
let MITM_SERVER_URL: String = ProcessInfo.processInfo.environment["ZONAROSA_MITM_URL"]
                              ?? "http://localhost:3737"

// MARK: - ZonaRosa MITM Server Constants
enum ZonaRosaMITMConfig {

  // The base URL all API clients will use — points to the MITM proxy
  static let serverURL = URL(string: MITM_SERVER_URL)!

  // WebSocket endpoint for real-time message delivery
  static var webSocketURL: URL {
    var comps        = URLComponents(url: serverURL, resolvingAgainstBaseURL: false)!
    comps.scheme     = serverURL.scheme == "https" ? "wss" : "ws"
    comps.path       = "/v1/websocket"
    return comps.url!
  }

  // CDN stub — in intercept mode point to proxy too so attachments are logged
  static let cdnURL        = URL(string: MITM_SERVER_URL + "/cdn")!
  static let cdn2URL       = URL(string: MITM_SERVER_URL + "/cdn2")!
  static let storageURL    = URL(string: MITM_SERVER_URL + "/storage")!

  // TLS pinning — disabled in intercept mode so proxy cert is accepted
  static let certificatePinning: Bool = false

  // Mark all connections as "trusted" in intercept mode
  static let isInterceptMode: Bool = true
}

// MARK: - URLSession configured for MITM proxy
extension URLSession {
  /// A session with TLS pinning disabled for MITM mode.
  static var zonarosaInterceptSession: URLSession = {
    let config = URLSessionConfiguration.default
    config.timeoutIntervalForRequest  = 30
    config.timeoutIntervalForResource = 60
    // Inject custom sender header so the dashboard can identify the device
    config.httpAdditionalHeaders = [
      "X-ZonaRosa-Agent": "iOS/\(UIDevice.current.systemVersion) ZonaRosa/MITM",
      "X-ZonaRosa-Sender": UIDevice.current.name,
    ]
    return URLSession(configuration: config)
  }()
}

// MARK: - How to wire this up
//
// In your AppEnvironment or SignalServiceKit setup, wherever you build
// the SSKEnvironment / OWSSignalService configuration, replace:
//
//   let serverURL = URL(string: TSConstants.mainServiceURL)!
//
// with:
//
//   let serverURL = ZonaRosaMITMConfig.serverURL
//
// And pass `ZonaRosaMITMConfig.certificatePinning` to your TLS stack.
//
// For a build-variant approach (recommended), wrap in a compiler flag:
//
//   #if MITM_MODE
//   let env = ZonaRosaMITMServerEnvironment()
//   #else
//   let env = ZonaRosaProductionEnvironment()
//   #endif
//
// Then add OTHER_SWIFT_FLAGS = -DMITM_MODE to your MITM scheme Build Settings.
