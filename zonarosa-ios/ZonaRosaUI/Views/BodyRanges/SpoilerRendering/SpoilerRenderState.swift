//
// Copyright 2023 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
public import ZonaRosaServiceKit

/// Container for stateful objects needed to render spoilers.
public class SpoilerRenderState {
    public let revealState: SpoilerRevealState
    public let animationManager: SpoilerAnimationManager

    public init() {
        self.revealState = SpoilerRevealState()
        self.animationManager = SpoilerAnimationManager()
    }
}
