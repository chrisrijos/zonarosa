//
// Copyright 2025 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

use libzonarosa_net_chat::api::RequestError;
use libzonarosa_net_chat::api::keytrans::Error as KeyTransError;

use crate::*;

#[bridge_fn]
fn TESTING_KeyTransFatalVerificationFailure() -> Result<(), RequestError<KeyTransError>> {
    Err(RequestError::Other(
        libzonarosa_keytrans::Error::VerificationFailed(
            "this is a fatal verification failure".to_string(),
        )
        .into(),
    ))
}

#[bridge_fn]
fn TESTING_KeyTransNonFatalVerificationFailure() -> Result<(), RequestError<KeyTransError>> {
    Err(RequestError::Other(
        libzonarosa_keytrans::Error::BadData("this is a non-fatal verification failure".to_string())
            .into(),
    ))
}

#[bridge_fn]
fn TESTING_KeyTransChatSendError() -> Result<(), RequestError<KeyTransError>> {
    Err(RequestError::Timeout)
}
