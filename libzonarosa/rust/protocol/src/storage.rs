//
// Copyright 2020-2022 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

//! Interfaces in [traits] and reference implementations in [inmem] for various mutable stores.

#![warn(missing_docs)]

mod inmem;
mod traits;

pub use inmem::{
    InMemIdentityKeyStore, InMemKyberPreKeyStore, InMemPreKeyStore, InMemSenderKeyStore,
    InMemSessionStore, InMemZonaRosaProtocolStore, InMemSignedPreKeyStore,
};
pub use traits::{
    Direction, IdentityChange, IdentityKeyStore, KyberPreKeyStore, PreKeyStore, ProtocolStore,
    SenderKeyStore, SessionStore, SignedPreKeyStore,
};
