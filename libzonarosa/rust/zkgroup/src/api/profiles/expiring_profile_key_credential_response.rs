//
// Copyright 2022 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

use partial_default::PartialDefault;
use serde::{Deserialize, Serialize};

use crate::common::serialization::ReservedByte;
use crate::common::simple_types::*;
use crate::crypto;

#[derive(Clone, Serialize, Deserialize, PartialDefault)]
pub struct ExpiringProfileKeyCredentialResponse {
    pub(crate) reserved: ReservedByte,
    pub(crate) blinded_credential: crypto::credentials::BlindedExpiringProfileKeyCredential,
    pub(crate) credential_expiration_time: Timestamp,
    pub(crate) proof: crypto::proofs::ExpiringProfileKeyCredentialIssuanceProof,
}
