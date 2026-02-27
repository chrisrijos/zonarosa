//
// Copyright 2025 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

public struct OWSPercentFormatStyle: FormatStyle {
    private let fractionDigits: Int

    public init(fractionDigits: Int = 0) {
        self.fractionDigits = fractionDigits
    }

    public func format(_ percent: Float) -> String {
        return percent.formatted(.percent.precision(.fractionLength(fractionDigits)))
    }
}

extension FormatStyle where Self == OWSPercentFormatStyle {
    public static func owsPercent(
        fractionDigits: Int = 0,
    ) -> OWSPercentFormatStyle {
        return OWSPercentFormatStyle(fractionDigits: fractionDigits)
    }
}
