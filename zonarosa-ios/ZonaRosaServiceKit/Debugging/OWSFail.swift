//
// Copyright 2023 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation

/// Log an error message. Additionally, crashes in prerelease builds.
@inlinable
public func owsFailBeta(
    _ logMessage: String,
    logger: PrefixedLogger = .empty(),
    file: String = #fileID,
    function: String = #function,
    line: Int = #line,
) {
    if BuildFlags.isPrerelease {
        owsFail(logMessage, logger: logger, file: file, function: function, line: line)
    } else {
        owsFailDebug(logMessage, logger: logger, file: file, function: function, line: line)
    }
}

/// Check an assertion. If the assertion fails, log an error message. Additionally, crashes in
/// prerelease builds.
@inlinable
public func owsAssertBeta(
    _ condition: Bool,
    _ message: @autoclosure () -> String = String(),
    file: String = #fileID,
    function: String = #function,
    line: Int = #line,
) {
    if !condition {
        let message: String = message()
        owsFailBeta(
            message.isEmpty ? "Assertion failed." : message,
            file: file,
            function: function,
            line: line,
        )
    }
}
