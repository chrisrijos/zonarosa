//
// Copyright 2024 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

use std::time::SystemTime;

pub(crate) trait Expireable {
    fn valid_at(&self, timestamp: SystemTime) -> bool;
}
