//
// Copyright 2020 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import LibZonaRosaClient
import XCTest

class TestCaseBase: XCTestCase {
    // Use a static stored property for one-time initialization.
    static let loggingInitialized: Bool = {
        struct LogToNSLog: LibzonarosaLogger {
            func log(level: LibzonarosaLogLevel, file: UnsafePointer<CChar>?, line: UInt32, message: UnsafePointer<CChar>)
            {
                let abbreviation: String
                switch level {
                case .error: abbreviation = "E"
                case .warn: abbreviation = "W"
                case .info: abbreviation = "I"
                case .debug: abbreviation = "D"
                case .trace: abbreviation = "T"
                }
                let file = file.map { String(cString: $0) } ?? "<unknown>"
                NSLog("%@ [%@:%u] %s", abbreviation, file, line, message)
            }

            func flush() {}
        }
        LogToNSLog().setUpLibzonarosaLogging(level: .trace)
        return true
    }()

    override class func setUp() {
        precondition(self.loggingInitialized)
    }

    internal func nonHermeticTest() throws {
        let varName = "LIBZONAROSA_TESTING_RUN_NONHERMETIC_TESTS"
        if ProcessInfo.processInfo.environment[varName] == nil {
            throw XCTSkip("requires \(varName)")
        }
    }
}
