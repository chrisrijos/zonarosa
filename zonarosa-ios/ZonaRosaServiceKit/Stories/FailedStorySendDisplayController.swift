//
// Copyright 2023 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation

/// Failed story send notifications check if the topmost view controller conforms
/// to this protocol.
/// In practice this is always ``MyStoriesViewController``, but that lives in
/// the ZonaRosa target and this needs to be checked in ZonaRosaMessaging.
public protocol FailedStorySendDisplayController: UIViewController {}
