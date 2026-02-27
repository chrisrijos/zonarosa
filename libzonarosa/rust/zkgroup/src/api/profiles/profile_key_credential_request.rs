//
// Copyright 2020 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

use partial_default::PartialDefault;
use serde::{Deserialize, Serialize};

use crate::common::serialization::ReservedByte;
use crate::crypto;

#[derive(Clone, Serialize, Deserialize, PartialDefault)]
pub struct ProfileKeyCredentialRequest {
    pub(crate) reserved: ReservedByte,
    pub(crate) public_key: crypto::profile_key_credential_request::PublicKey,
    pub(crate) ciphertext: crypto::profile_key_credential_request::Ciphertext,
    pub(crate) proof: crypto::proofs::ProfileKeyCredentialRequestProof,
}
