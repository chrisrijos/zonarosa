//
// Copyright 2023 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import XCTest

@testable import ZonaRosaServiceKit

final class CallRecordTest: XCTestCase {
    func testNoOverlapBetweenStatuses() {
        let allIndividualStatusRawValues = CallRecord.CallStatus.IndividualCallStatus.allCases.map { $0.rawValue }
        let allGroupStatusRawValues = CallRecord.CallStatus.GroupCallStatus.allCases.map { $0.rawValue }

        XCTAssertFalse(
            Set(allIndividualStatusRawValues).intersects(Set(allGroupStatusRawValues)),
        )
    }
}
