//
// Copyright 2020 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

use partial_default::PartialDefault;
use serde::{Deserialize, Serialize};

use crate::common::serialization::ReservedByte;
use crate::crypto;

#[derive(Copy, Clone, Serialize, Deserialize, PartialDefault)]
pub struct ProfileKeyCommitment {
    pub(crate) reserved: ReservedByte,
    pub(crate) commitment: crypto::profile_key_commitment::Commitment,
}
