//
// Copyright 2023 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation

public enum StoryContextViewState: Equatable {
    case unviewed
    case viewed
    case noStories

    var hasStoriesToDisplay: Bool {
        switch self {
        case .noStories: return false
        case .viewed, .unviewed: return true
        }
    }
}
