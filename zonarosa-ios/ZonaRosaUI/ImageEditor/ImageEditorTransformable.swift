//
// Copyright 2023 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation

protocol ImageEditorTransformable: ImageEditorItem {
    var unitCenter: ImageEditorSample { get }
    var scaling: CGFloat { get }
    var rotationRadians: CGFloat { get }
    func copy(unitCenter: CGPoint) -> Self
    func copy(scaling: CGFloat, rotationRadians: CGFloat) -> Self
}
