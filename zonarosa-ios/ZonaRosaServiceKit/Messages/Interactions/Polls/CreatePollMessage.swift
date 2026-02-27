//
// Copyright 2025 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

public struct CreatePollMessage {
    let question: String
    let options: [String]
    let allowMultiple: Bool

    public init(
        question: String,
        options: [String],
        allowMultiple: Bool,
    ) {
        self.question = question
        self.options = options
        self.allowMultiple = allowMultiple
    }
}
