//
// Copyright 2020 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

public import XCTest

@testable import ZonaRosaServiceKit

open class ZonaRosaBaseTest: XCTestCase {
    private var oldContext: (any AppContext)!

    @MainActor
    override public func setUp() {
        super.setUp()
        let setupExpectation = expectation(description: "mock ssk environment setup completed")
        self.oldContext = CurrentAppContext()
        Task {
            await MockSSKEnvironment.activate()
            setupExpectation.fulfill()
        }
        waitForExpectations(timeout: 30)
    }

    @MainActor
    override open func tearDown() {
        MockSSKEnvironment.deactivate(oldContext: self.oldContext)
        super.tearDown()
    }

    func read<T>(block: (DBReadTransaction) throws -> T) rethrows -> T {
        return try SSKEnvironment.shared.databaseStorageRef.read(block: block)
    }

    func write<T>(block: (DBWriteTransaction) throws -> T) rethrows -> T {
        return try SSKEnvironment.shared.databaseStorageRef.write(block: block)
    }
}
