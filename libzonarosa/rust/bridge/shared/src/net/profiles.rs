//
// Copyright 2026 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

use std::convert::Infallible;

use libzonarosa_bridge_macros::bridge_io;
use libzonarosa_bridge_types::net::TokioAsyncContext;
use libzonarosa_bridge_types::net::chat::UnauthenticatedChatConnection;
use libzonarosa_bridge_types::*;
use libzonarosa_core::ServiceId;
use libzonarosa_net_chat::api::RequestError;
use libzonarosa_net_chat::api::profiles::UnauthenticatedAccountExistenceApi;

use crate::support::*;

#[bridge_io(TokioAsyncContext)]
async fn UnauthenticatedChatConnection_account_exists(
    chat: &UnauthenticatedChatConnection,
    account: ServiceId,
) -> Result<bool, RequestError<Infallible>> {
    chat.as_typed(|chat| chat.account_exists(account)).await
}
