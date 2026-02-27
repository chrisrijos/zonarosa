//
// Copyright 2024 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

use libzonarosa_bridge_macros::bridge_fn;
use libzonarosa_bridge_types::net::tokio::TokioAsyncContext;

use crate::support::*;
use crate::*;

bridge_handle_fns!(TokioAsyncContext, clone = false);

#[bridge_fn]
fn TokioAsyncContext_new() -> TokioAsyncContext {
    TokioAsyncContext::new()
}

#[bridge_fn]
fn TokioAsyncContext_cancel(context: &TokioAsyncContext, raw_cancellation_id: u64) {
    context.cancel(raw_cancellation_id.into())
}
