//
// Copyright 2023 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation

public enum AccountDataReportRequestFactory {
    public static func createAccountDataReportRequest() -> TSRequest {
        let url = URL(pathComponents: ["v2", "accounts", "data_report"])!
        return TSRequest(url: url, method: "GET", parameters: nil)
    }
}
