//
// Copyright 2025 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Testing

@testable import ZonaRosaServiceKit

@Suite
struct ObjectRetainerTest {
    private class Retainer {}
    private class Retained {}

    @Test
    func retainerRetainsObjectUntilRetainerReleased() {
        var retainer: Retainer? = Retainer()
        weak var retained: Retained?

        do {
            let _retained = Retained()
            retained = _retained

            ObjectRetainer.retainObject(
                retained!,
                forLifetimeOf: retainer!,
            )
        }

        #expect(retainer != nil)
        #expect(retained != nil)

        retainer = nil

        #expect(retainer == nil)
        #expect(retained == nil)
    }
}
