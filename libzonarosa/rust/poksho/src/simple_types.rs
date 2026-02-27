//
// Copyright 2020 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

use curve25519_dalek::ristretto::RistrettoPoint;
use curve25519_dalek::scalar::Scalar;

pub type G1 = Vec<Scalar>; // Schnorr proves preimage of homomorphism from G1 -> G2
pub type G2 = Vec<RistrettoPoint>;
