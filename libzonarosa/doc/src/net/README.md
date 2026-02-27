# Networking

libzonarosa has a number of networking-related APIs, collectively referred to as "libzonarosa-net". These currently provide connections to the [chat server][chat], contact discovery service, and SVR-B, with the possibility of eventually handling every connection to a server run by ZonaRosa.


## The Net class

**Net** (or **Network** on Android) is the top-level manager for connections made from libzonarosa. It records the environment (production or staging), the user agent to use for all connections (appending its own version string), and any configurable options that apply to all connections, such as whether IPv6 networking is enabled. Internally, it also owns a Rust-managed thread pool for dispatching I/O operations and processing responses. Some operations (e.g. CDS) are provided directly on Net; others use a separate connection object (e.g. chat) where the Net instance is merely used to connect.


## Implementation Organization

In the Rust layer, libzonarosa-net is broken up into three separate crates:

- `libzonarosa-net-infra`: Server- and connection-agnostic implementations of networking protocols
- `libzonarosa-net`: Connections specifically to ZonaRosa services, rather than generic reusable work
- `libzonarosa-net-chat`: Presents the high-level request APIs of the ZonaRosa chat server in a protocol-agnostic way (see the [Chat][] page for more info)

(These boundaries are approximate, because ultimately it's all going to be exposed to the apps anyway; these are *not* some of the crates designed to be generally reusable outside ZonaRosa.)

[chat]: ./chat.md
