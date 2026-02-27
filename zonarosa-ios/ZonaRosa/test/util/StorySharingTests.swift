//
// Copyright 2022 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import LibZonaRosaClient
import XCTest

@testable import ZonaRosaServiceKit
@testable import ZonaRosaUI

class StorySharingTests: ZonaRosaBaseTest {
    func testUrlStripping() {
        let inputOutput = [
            "https://zonarosa.io test": "test",
            "https://zonarosa.iotest test https://zonarosa.io": "https://zonarosa.iotest test",
            "testhttps://zonarosa.io": "testhttps://zonarosa.io",
            "test\nhttps://zonarosa.io": "test",
            "https://zonarosa.io\ntest": "test",
            "https://zonarosa.io\ntest\nhttps://zonarosa.io": "test\nhttps://zonarosa.io",
            "some https://zonarosa.io test": "some https://zonarosa.io test",
            "https://zonarosa.io": nil,
            "something else": "something else",
        ]

        for (input, expectedOutput) in inputOutput {
            let output = StorySharing.text(
                for: .init(
                    text: input,
                    ranges: .empty,
                ),
                with: OWSLinkPreviewDraft(
                    url: URL(string: "https://zonarosa.io")!,
                    title: nil,
                    isForwarded: false,
                ),
            )?.text
            XCTAssertEqual(output, expectedOutput)
        }
    }

    func testMentionFlattening() {
        let mentionAci = Aci.randomForTesting()
        let range = NSRange(location: 0, length: MessageBody.mentionPlaceholder.utf16.count)
        let output = StorySharing.text(
            for: .init(
                text: "\(MessageBody.mentionPlaceholder) Some text",
                ranges: .init(mentions: [range: mentionAci], styles: []),
            ),
            with: nil,
        )?.text

        XCTAssertEqual(output, "@Unknown Some text")
    }
}
