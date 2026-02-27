//
// Copyright 2023 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

public import ZonaRosaServiceKit

extension HydratedMessageBody.DisplayConfiguration.SearchRanges {

    public static func matchedRanges(_ ranges: [NSRange]) -> Self {
        return HydratedMessageBody.DisplayConfiguration.SearchRanges(
            matchingBackgroundColor: .fixed(ConversationStyle.searchMatchHighlightColor),
            matchingForegroundColor: .fixed(.ows_black),
            matchedRanges: ranges,
        )
    }
}
