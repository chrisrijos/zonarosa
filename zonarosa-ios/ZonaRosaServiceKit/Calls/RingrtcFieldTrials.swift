//
// Copyright 2023 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation

/// Utilities for loading RingRTC field trials from ``RemoteConfig``s.
public enum RingrtcFieldTrials {
    public static func trials(with remoteConfig: RemoteConfig) -> [String: String] {
        var result = [String: String]()

        if remoteConfig.ringrtcNwPathMonitorTrial {
            result["WebRTC-Network-UseNWPathMonitor"] = "Enabled"
        }

        return result
    }
}
