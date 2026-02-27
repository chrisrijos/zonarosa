//
// Copyright 2020 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

use partial_default::PartialDefault;
use serde::{Deserialize, Serialize};

use crate::common::serialization::ReservedByte;
use crate::crypto;

#[derive(Copy, Clone, Serialize, Deserialize, PartialEq, Eq, PartialDefault)]
pub struct UuidCiphertext {
    pub(crate) reserved: ReservedByte,
    pub(crate) ciphertext: crypto::uid_encryption::Ciphertext,
}
