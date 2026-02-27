//
// Copyright 2023 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

#![allow(clippy::new_without_default)]

pub use libzonarosa_protocol_current::{
    CiphertextMessageType, PreKeyBundle, UnidentifiedSenderMessageContent,
};

pub trait LibZonaRosaProtocolStore {
    fn version(&self) -> &'static str;
    fn create_pre_key_bundle(&mut self) -> PreKeyBundle;
    fn process_pre_key_bundle(&mut self, remote: &str, pre_key_bundle: PreKeyBundle);
    fn encrypt(&mut self, remote: &str, msg: &[u8]) -> (Vec<u8>, CiphertextMessageType);
    fn decrypt(&mut self, remote: &str, msg: &[u8], msg_type: CiphertextMessageType) -> Vec<u8>;

    fn encrypt_sealed_sender_v1(
        &self,
        remote: &str,
        msg: &UnidentifiedSenderMessageContent,
    ) -> Vec<u8>;
    fn encrypt_sealed_sender_v2(
        &self,
        remote: &str,
        msg: &UnidentifiedSenderMessageContent,
    ) -> Vec<u8>;
    fn decrypt_sealed_sender(&self, msg: &[u8]) -> UnidentifiedSenderMessageContent;
}

mod current;
pub use current::LibZonaRosaProtocolCurrent;

mod v70;
pub use v70::LibZonaRosaProtocolV70;

// Use this function to debug tests
pub fn init_test_logger() {
    let _ = env_logger::builder()
        .filter_level(log::LevelFilter::max())
        .is_test(true)
        .try_init();
}

pub fn try_all_combinations(
    f: fn(&mut dyn LibZonaRosaProtocolStore, &mut dyn LibZonaRosaProtocolStore),
    make_previous: &[fn() -> Box<dyn LibZonaRosaProtocolStore>],
) {
    let run = |alice_store: &mut dyn LibZonaRosaProtocolStore,
               bob_store: &mut dyn LibZonaRosaProtocolStore| {
        log::info!(
            "alice: {}, bob: {}",
            alice_store.version(),
            bob_store.version()
        );
        f(alice_store, bob_store)
    };

    // Current<->Current, to test that the test is correct.
    run(
        &mut LibZonaRosaProtocolCurrent::new(),
        &mut LibZonaRosaProtocolCurrent::new(),
    );

    // Current<->Previous
    for bob_store_maker in make_previous {
        let mut alice_store = LibZonaRosaProtocolCurrent::new();
        let mut bob_store = bob_store_maker();
        run(&mut alice_store, &mut *bob_store);
    }

    // Previous<->Current
    for alice_store_maker in make_previous {
        let mut alice_store = alice_store_maker();
        let mut bob_store = LibZonaRosaProtocolCurrent::new();
        run(&mut *alice_store, &mut bob_store);
    }
}
