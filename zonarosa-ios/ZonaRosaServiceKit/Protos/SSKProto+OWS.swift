//
// Copyright 2019 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import LibZonaRosaClient

@objc
public extension SSKProtoSyncMessageSent {
    var isStoryTranscript: Bool {
        storyMessage != nil || !storyMessageRecipients.isEmpty
    }
}
