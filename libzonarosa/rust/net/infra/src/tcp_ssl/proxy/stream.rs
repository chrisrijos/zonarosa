//
// Copyright 2024 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

use auto_enums::enum_derive;
use tokio_boring_zonarosa::SslStream;

use crate::Connection;
use crate::tcp_ssl::TcpStream;
use crate::tcp_ssl::proxy::https::HttpProxyStream;
use crate::tcp_ssl::proxy::socks::SocksStream;

#[derive(Debug, derive_more::From)]
#[enum_derive(tokio1::AsyncRead, tokio1::AsyncWrite)]
pub enum ProxyStream {
    Tls(SslStream<TcpStream>),
    Tcp(TcpStream),
    Socks(SocksStream<TcpStream>),
    Http(HttpProxyStream),
}

impl Connection for ProxyStream {
    fn transport_info(&self) -> crate::TransportInfo {
        match self {
            ProxyStream::Tls(ssl_stream) => ssl_stream.transport_info(),
            ProxyStream::Tcp(tcp_stream) => tcp_stream.transport_info(),
            ProxyStream::Socks(either) => either.transport_info(),
            ProxyStream::Http(http) => http.transport_info(),
        }
    }
}
