//
// Copyright 2026 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation

/// Utility for deciding whether to offer VP9
public enum RingrtcVp9Config {
    public static func enableVp9(with remoteConfig: RemoteConfig) -> Bool {
        if DebugFlags.callingForceVp9On.get() {
            return true
        }
        if DebugFlags.callingForceVp9Off.get() {
            return false
        }

        guard let hwIdentifier = String(sysctlKey: "hw.machine") else {
            return false
        }

        if remoteConfig.ringrtcVp9DeviceModelDenylist.contains(hwIdentifier) {
            return false
        } else if remoteConfig.ringrtcVp9DeviceModelEnablelist.contains(hwIdentifier) {
            return true
        }
        return remoteConfig.ringrtcVp9Enabled
    }
}
