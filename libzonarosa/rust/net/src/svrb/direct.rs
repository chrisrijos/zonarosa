//
// Copyright 2024 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

use http::HeaderName;
use libzonarosa_net_infra::EnableDomainFronting;
use libzonarosa_net_infra::dns::DnsResolver;
use libzonarosa_net_infra::route::DirectOrProxyProvider;
use libzonarosa_net_infra::utils::NetworkChangeEvent;

use crate::auth::Auth;
use crate::connect_state::{ConnectState, ConnectionResources, SUGGESTED_CONNECT_CONFIG};
use crate::enclave::{self, EnclaveEndpoint, NewHandshake, SvrBFlavor};
use crate::svr::SvrConnection;

pub async fn direct_connect<Enclave>(
    endpoint: &EnclaveEndpoint<'static, Enclave>,
    auth: &Auth,
    network_change_event: &NetworkChangeEvent,
) -> Result<SvrConnection<Enclave>, enclave::Error>
where
    Enclave: SvrBFlavor + NewHandshake + Sized,
{
    let confirmation_header_name = endpoint
        .domain_config
        .connect
        .confirmation_header_name
        .map(HeaderName::from_static);
    let connect_state = ConnectState::new(SUGGESTED_CONNECT_CONFIG);
    let resolver = DnsResolver::new(network_change_event);
    let connection_resources = ConnectionResources {
        connect_state: &connect_state,
        dns_resolver: &resolver,
        network_change_event,
        confirmation_header_name,
    };

    SvrConnection::connect(
        connection_resources,
        DirectOrProxyProvider::direct(
            endpoint.enclave_websocket_provider(EnableDomainFronting::No),
        ),
        endpoint.ws_config,
        &endpoint.params,
        auth,
    )
    .await
}
