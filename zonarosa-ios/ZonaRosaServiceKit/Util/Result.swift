//
// Copyright 2023 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation

extension Result {
    public init(catching block: () async throws(Failure) -> Success) async {
        do throws(Failure) {
            self = .success(try await block())
        } catch {
            self = .failure(error)
        }
    }
}

extension Result {
    public var isSuccess: Bool {
        switch self {
        case .success:
            return true
        case .failure:
            return false
        }
    }
}
