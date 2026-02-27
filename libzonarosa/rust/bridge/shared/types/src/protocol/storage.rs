//
// Copyright 2026 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

use async_trait::async_trait;
use libzonarosa_bridge_macros::bridge_callbacks;
use libzonarosa_protocol::{
    KyberPreKeyId, KyberPreKeyRecord, KyberPreKeyStore, PreKeyId, PreKeyRecord, PreKeyStore,
    PublicKey, ZonaRosaProtocolError, SignedPreKeyId, SignedPreKeyRecord, SignedPreKeyStore,
};

use crate::support::{BridgedCallbacks, ResultLike, WithContext};
use crate::*;

/// A bridge-friendly version of [`PreKeyStore`].
#[bridge_callbacks(jni = "io.zonarosa.libzonarosa.protocol.state.internal.PreKeyStore")]
pub(crate) trait BridgePreKeyStore {
    async fn load_pre_key(&self, id: u32) -> Result<Option<PreKeyRecord>, ZonaRosaProtocolError>;
    async fn store_pre_key(&self, id: u32, record: PreKeyRecord)
    -> Result<(), ZonaRosaProtocolError>;
    async fn remove_pre_key(&self, id: u32) -> Result<(), ZonaRosaProtocolError>;
}

#[async_trait(?Send)]
impl<T: BridgePreKeyStore> PreKeyStore for BridgedCallbacks<T> {
    async fn get_pre_key(&self, pre_key_id: PreKeyId) -> Result<PreKeyRecord, ZonaRosaProtocolError> {
        self.0
            .load_pre_key(pre_key_id.into())
            .await?
            .ok_or(ZonaRosaProtocolError::InvalidPreKeyId)
    }

    async fn save_pre_key(
        &mut self,
        pre_key_id: PreKeyId,
        record: &PreKeyRecord,
    ) -> Result<(), ZonaRosaProtocolError> {
        self.0
            .store_pre_key(pre_key_id.into(), record.clone())
            .await
    }

    async fn remove_pre_key(&mut self, pre_key_id: PreKeyId) -> Result<(), ZonaRosaProtocolError> {
        self.0.remove_pre_key(pre_key_id.into()).await
    }
}

/// A bridge-friendly version of [`SignedPreKeyStore`].
#[bridge_callbacks(jni = "io.zonarosa.libzonarosa.protocol.state.internal.SignedPreKeyStore")]
pub(crate) trait BridgeSignedPreKeyStore {
    async fn load_signed_pre_key(
        &self,
        id: u32,
    ) -> Result<Option<SignedPreKeyRecord>, ZonaRosaProtocolError>;
    async fn store_signed_pre_key(
        &self,
        id: u32,
        record: SignedPreKeyRecord,
    ) -> Result<(), ZonaRosaProtocolError>;
}

#[async_trait(?Send)]
impl<T: BridgeSignedPreKeyStore> SignedPreKeyStore for BridgedCallbacks<T> {
    /// Look up the signed pre-key corresponding to `signed_prekey_id`.
    async fn get_signed_pre_key(
        &self,
        signed_prekey_id: SignedPreKeyId,
    ) -> Result<SignedPreKeyRecord, ZonaRosaProtocolError> {
        self.0
            .load_signed_pre_key(signed_prekey_id.into())
            .await?
            .ok_or(ZonaRosaProtocolError::InvalidSignedPreKeyId)
    }

    /// Set the entry for `signed_prekey_id` to the value of `record`.
    async fn save_signed_pre_key(
        &mut self,
        signed_prekey_id: SignedPreKeyId,
        record: &SignedPreKeyRecord,
    ) -> Result<(), ZonaRosaProtocolError> {
        self.0
            .store_signed_pre_key(signed_prekey_id.into(), record.clone())
            .await
    }
}

/// A bridge-friendly version of [`KyberPreKeyStore`].
#[bridge_callbacks(jni = "io.zonarosa.libzonarosa.protocol.state.internal.KyberPreKeyStore")]
pub(crate) trait BridgeKyberPreKeyStore {
    async fn load_kyber_pre_key(
        &self,
        id: u32,
    ) -> Result<Option<KyberPreKeyRecord>, ZonaRosaProtocolError>;
    async fn store_kyber_pre_key(
        &self,
        id: u32,
        record: KyberPreKeyRecord,
    ) -> Result<(), ZonaRosaProtocolError>;
    async fn mark_kyber_pre_key_used(
        &self,
        id: u32,
        ec_prekey_id: u32,
        base_key: PublicKey,
    ) -> Result<(), ZonaRosaProtocolError>;
}

#[async_trait(?Send)]
impl<T: BridgeKyberPreKeyStore> KyberPreKeyStore for BridgedCallbacks<T> {
    async fn get_kyber_pre_key(
        &self,
        id: KyberPreKeyId,
    ) -> Result<KyberPreKeyRecord, ZonaRosaProtocolError> {
        BridgeKyberPreKeyStore::load_kyber_pre_key(&self.0, id.into())
            .await?
            .ok_or(ZonaRosaProtocolError::InvalidKyberPreKeyId)
    }

    async fn save_kyber_pre_key(
        &mut self,
        id: KyberPreKeyId,
        record: &KyberPreKeyRecord,
    ) -> Result<(), ZonaRosaProtocolError> {
        BridgeKyberPreKeyStore::store_kyber_pre_key(&self.0, id.into(), record.clone()).await
    }

    async fn mark_kyber_pre_key_used(
        &mut self,
        id: KyberPreKeyId,
        ec_prekey_id: SignedPreKeyId,
        base_key: &PublicKey,
    ) -> Result<(), ZonaRosaProtocolError> {
        BridgeKyberPreKeyStore::mark_kyber_pre_key_used(
            &self.0,
            id.into(),
            ec_prekey_id.into(),
            *base_key,
        )
        .await
    }
}
