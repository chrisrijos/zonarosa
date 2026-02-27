//
// Copyright 2021 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import UIKit

// Any view that exposes a read-only image that can be used for transitions
public protocol PrimaryImageView: UIView {
    var primaryImage: UIImage? { get }
}

extension UIImageView: PrimaryImageView {
    public var primaryImage: UIImage? { image }
}
