//
// Copyright 2024 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import ZonaRosaFfi

public protocol ChatConnection: AnyObject {
    /// Initiates termination of the underlying connection to the Chat Service.
    ///
    /// Returns when the disconnection is complete.
    func disconnect() async throws

    /// Sends a request to the Chat Service.
    ///
    /// - Throws: ``ZonaRosaError``s for the various failure modes.
    func send(_ request: Request) async throws -> Response

    /// Produces information about the connection.
    func info() -> ConnectionInfo
}

public class ConnectionInfo: NativeHandleOwner<ZonaRosaMutPointerChatConnectionInfo>, CustomStringConvertible {
    override class func destroyNativeHandle(_ handle: NonNull<ZonaRosaMutPointerChatConnectionInfo>) -> ZonaRosaFfiErrorRef?
    {
        // ChatConnectionInfo is an alias for ConnectionInfo, but Swift doesn't know that.
        return zonarosa_connection_info_destroy(ZonaRosaMutPointerConnectionInfo(raw: handle.opaque))
    }

    /// The local port used by the connection.
    public var localPort: UInt16 {
        withNativeHandle { connectionInfo in
            failOnError {
                try invokeFnReturningInteger {
                    zonarosa_chat_connection_info_local_port($0, connectionInfo.const())
                }
            }
        }
    }

    /// The IP addressing version used by the connection.
    public var ipType: IpType {
        let rawValue = withNativeHandle { connectionInfo in
            failOnError {
                try invokeFnReturningInteger {
                    zonarosa_chat_connection_info_ip_version($0, connectionInfo.const())
                }
            }
        }
        return IpType(rawValue: rawValue) ?? .unknown
    }

    /// A developer-facing description of the connection.
    public var description: String {
        withNativeHandle { connectionInfo in
            failOnError {
                try invokeFnReturningString {
                    zonarosa_chat_connection_info_description($0, connectionInfo.const())
                }
            }
        }
    }
}

extension ZonaRosaMutPointerChatConnectionInfo: ZonaRosaMutPointer {
    public typealias ConstPointer = ZonaRosaConstPointerChatConnectionInfo

    public init(untyped: OpaquePointer?) {
        self.init(raw: untyped)
    }

    public func toOpaque() -> OpaquePointer? {
        self.raw
    }

    public func const() -> Self.ConstPointer {
        Self.ConstPointer(raw: self.raw)
    }
}

extension ZonaRosaConstPointerChatConnectionInfo: ZonaRosaConstPointer {
    public func toOpaque() -> OpaquePointer? {
        self.raw
    }
}

extension ChatConnection {
    public typealias Request = ChatRequest
    public typealias Response = ChatResponse
}

/// Represents an authenticated connection to the Chat Service.
///
/// An instance of this object is obtained via call to ``Net/connectAuthenticatedChat(username:password:receiveStories:languages:)``.
/// Before an obtained instance can be used, it must be started by calling ``AuthenticatedChatConnection/start(listener:)``.
public class AuthenticatedChatConnection: NativeHandleOwner<
    ZonaRosaMutPointerAuthenticatedChatConnection
>, ChatConnection, @unchecked Sendable
{
    internal let tokioAsyncContext: TokioAsyncContext

    /// Initiates establishing of the underlying unauthenticated connection to the Chat Service. Once
    /// the connection is established, the returned object can be used to send and receive messages
    /// after ``AuthenticatedChatConnection/start(listener:)`` is called.
    internal init(
        tokioAsyncContext: TokioAsyncContext,
        connectionManager: ConnectionManager,
        username: String,
        password: String,
        receiveStories: Bool,
        languages: [String]
    ) async throws {
        let nativeHandle = try await tokioAsyncContext.invokeAsyncFunction { promise, tokioAsyncContext in
            connectionManager.withNativeHandle { connectionManager in
                languages.withUnsafeBorrowedBytestringArray { languages in
                    zonarosa_authenticated_chat_connection_connect(
                        promise,
                        tokioAsyncContext.const(),
                        connectionManager.const(),
                        username,
                        password,
                        receiveStories,
                        languages
                    )
                }
            }
        }
        self.tokioAsyncContext = tokioAsyncContext
        super.init(owned: NonNull(nativeHandle)!)
    }

    override internal class func destroyNativeHandle(
        _ handle: NonNull<ZonaRosaMutPointerAuthenticatedChatConnection>
    ) -> ZonaRosaFfiErrorRef? {
        return zonarosa_authenticated_chat_connection_destroy(handle.pointer)
    }

    internal required init(owned handle: NonNull<ZonaRosaMutPointerAuthenticatedChatConnection>) {
        fatalError("should not be called directly for a ChatConnection")
    }

    internal init(
        fakeHandle handle: NonNull<ZonaRosaMutPointerAuthenticatedChatConnection>,
        tokioAsyncContext: TokioAsyncContext
    ) {
        self.tokioAsyncContext = tokioAsyncContext
        super.init(owned: handle)
    }

    /// Sets the listener and starts the background thread that handles communication.
    ///
    /// This must be called exactly once for the ``AuthenticatedChatConnection``
    /// to be used. Before this method is called, no messages can be sent or
    /// received.
    public func start(listener: any ChatConnectionListener) {
        withNativeHandle { chatConnection in
            var listenerStruct = ChatListenerBridge(chatConnection: self, chatListener: listener)
                .makeListenerStruct()
            withUnsafePointer(to: &listenerStruct) {
                failOnError(
                    zonarosa_authenticated_chat_connection_init_listener(
                        chatConnection.const(),
                        ZonaRosaConstPointerFfiChatListenerStruct(raw: $0)
                    )
                )
            }
        }
    }

    /// Initiates termination of the underlying connection to the Chat Service.
    ///
    /// Returns when the disconnection is complete.
    public func disconnect() async throws {
        _ = try await self.tokioAsyncContext.invokeAsyncFunction { promise, tokioAsyncContext in
            withNativeHandle { chatConnection in
                zonarosa_authenticated_chat_connection_disconnect(
                    promise,
                    tokioAsyncContext.const(),
                    chatConnection.const()
                )
            }
        }
    }

    /// Sends a request to the Chat Service over an authenticated channel.
    ///
    /// - Throws: ``ZonaRosaError/chatServiceInactive(_:)`` if you haven't called ``start(listener:)``
    /// - Throws: Other ``ZonaRosaError``s for other kinds of failures.
    public func send(_ request: Request) async throws -> Response {
        let internalRequest = try Request.InternalRequest(request)
        let timeoutMillis = request.timeoutMillis
        let rawResponse: ZonaRosaFfiChatResponse = try await self.tokioAsyncContext
            .invokeAsyncFunction { promise, tokioAsyncContext in
                withNativeHandle { chatService in
                    internalRequest.withNativeHandle { request in
                        zonarosa_authenticated_chat_connection_send(
                            promise,
                            tokioAsyncContext.const(),
                            chatService.const(),
                            request.const(),
                            timeoutMillis
                        )
                    }
                }
            }
        return try Response(consuming: rawResponse)
    }

    /// Returns an object representing information about the connection.
    public func info() -> ConnectionInfo {
        withNativeHandle { chatConnection in
            failOnError {
                try invokeFnReturningNativeHandle {
                    zonarosa_authenticated_chat_connection_info($0, chatConnection.const())
                }
            }
        }
    }
}

extension ZonaRosaMutPointerAuthenticatedChatConnection: ZonaRosaMutPointer {
    public typealias ConstPointer = ZonaRosaConstPointerAuthenticatedChatConnection

    public init(untyped: OpaquePointer?) {
        self.init(raw: untyped)
    }

    public func toOpaque() -> OpaquePointer? {
        self.raw
    }

    public func const() -> Self.ConstPointer {
        Self.ConstPointer(raw: self.raw)
    }
}

extension ZonaRosaConstPointerAuthenticatedChatConnection: ZonaRosaConstPointer {
    public func toOpaque() -> OpaquePointer? {
        self.raw
    }
}

/// Represents an unauthenticated connection to the Chat Service.
///
/// An instance of this object is obtained via call to ``Net/connectUnauthenticatedChat(languages:)``.
/// Before an obtained instance can be used, it must be started by calling ``UnauthenticatedChatConnection/start(listener:)``.
public class UnauthenticatedChatConnection: NativeHandleOwner<
    ZonaRosaMutPointerUnauthenticatedChatConnection
>, ChatConnection, @unchecked Sendable
{
    internal let tokioAsyncContext: TokioAsyncContext
    internal let environment: Net.Environment

    /// Initiates establishing of the underlying unauthenticated connection to
    /// the Chat Service. Once the connection is established, the returned
    /// object can be used to send and receive messages after
    /// ``UnauthenticatedChatConnection/start(listener:)`` is called.
    internal init(
        tokioAsyncContext: TokioAsyncContext,
        connectionManager: ConnectionManager,
        languages: [String],
        environment: Net.Environment
    ) async throws {
        let nativeHandle = try await tokioAsyncContext.invokeAsyncFunction { promise, tokioAsyncContext in
            connectionManager.withNativeHandle { connectionManager in
                languages.withUnsafeBorrowedBytestringArray { languages in
                    zonarosa_unauthenticated_chat_connection_connect(
                        promise,
                        tokioAsyncContext.const(),
                        connectionManager.const(),
                        languages
                    )
                }
            }
        }
        self.tokioAsyncContext = tokioAsyncContext
        self.environment = environment
        super.init(owned: NonNull(nativeHandle)!)
    }

    internal init(
        fakeHandle handle: NonNull<ZonaRosaMutPointerUnauthenticatedChatConnection>,
        tokioAsyncContext: TokioAsyncContext,
        environment: Net.Environment
    ) {
        self.tokioAsyncContext = tokioAsyncContext
        self.environment = environment
        super.init(owned: handle)
    }

    override internal class func destroyNativeHandle(
        _ handle: NonNull<ZonaRosaMutPointerUnauthenticatedChatConnection>
    ) -> ZonaRosaFfiErrorRef? {
        return zonarosa_unauthenticated_chat_connection_destroy(handle.pointer)
    }

    internal required init(owned handle: NonNull<ZonaRosaMutPointerUnauthenticatedChatConnection>) {
        fatalError("should not be called directly for a ChatConnection")
    }

    /// Sets the listener and starts the background thread that handles communication.
    ///
    /// This must be called exactly once for the
    /// ``UnauthenticatedChatConnection`` to be used. Before this method is
    /// called, no messages can be sent or received.
    public func start(listener: any ConnectionEventsListener<UnauthenticatedChatConnection>) {
        withNativeHandle { chatConnection in
            var listenerStruct = UnauthConnectionEventsListenerBridge(
                chatConnection: self,
                listener: listener
            ).makeListenerStruct()
            withUnsafePointer(to: &listenerStruct) {
                failOnError(
                    zonarosa_unauthenticated_chat_connection_init_listener(
                        chatConnection.const(),
                        ZonaRosaConstPointerFfiChatListenerStruct(raw: $0)
                    )
                )
            }
        }
    }

    /// Initiates termination of the underlying connection to the Chat Service.
    ///
    /// Returns when the disconnection is complete.
    public func disconnect() async throws {
        _ = try await self.tokioAsyncContext.invokeAsyncFunction { promise, tokioAsyncContext in
            withNativeHandle { chatConnection in
                zonarosa_unauthenticated_chat_connection_disconnect(
                    promise,
                    tokioAsyncContext.const(),
                    chatConnection.const()
                )
            }
        }
    }

    /// Sends request to the Chat Service over an authenticated channel.
    ///
    /// - Throws: ``ZonaRosaError/chatServiceInactive(_:)`` if you haven't called ``start(listener:)``.
    /// - Throws: Other ``ZonaRosaError``s for other kinds of failures.
    public func send(_ request: Request) async throws -> Response {
        let internalRequest = try Request.InternalRequest(request)
        let timeoutMillis = request.timeoutMillis
        let rawResponse: ZonaRosaFfiChatResponse = try await self.tokioAsyncContext
            .invokeAsyncFunction { promise, tokioAsyncContext in
                withNativeHandle { chatService in
                    internalRequest.withNativeHandle { request in
                        zonarosa_unauthenticated_chat_connection_send(
                            promise,
                            tokioAsyncContext.const(),
                            chatService.const(),
                            request.const(),
                            timeoutMillis
                        )
                    }
                }
            }
        return try Response(consuming: rawResponse)
    }

    /// Returns an object representing information about the connection.
    public func info() -> ConnectionInfo {
        withNativeHandle { chatConnection in
            failOnError {
                try invokeFnReturningNativeHandle {
                    zonarosa_unauthenticated_chat_connection_info($0, chatConnection.const())
                }
            }
        }
    }
}

extension ZonaRosaMutPointerUnauthenticatedChatConnection: ZonaRosaMutPointer {
    public typealias ConstPointer = ZonaRosaConstPointerUnauthenticatedChatConnection

    public init(untyped: OpaquePointer?) {
        self.init(raw: untyped)
    }

    public func toOpaque() -> OpaquePointer? {
        self.raw
    }

    public func const() -> Self.ConstPointer {
        Self.ConstPointer(raw: self.raw)
    }
}

extension ZonaRosaConstPointerUnauthenticatedChatConnection: ZonaRosaConstPointer {
    public func toOpaque() -> OpaquePointer? {
        self.raw
    }
}

extension UnauthenticatedChatConnection {
    public var keyTransparencyClient: KeyTransparency.Client {
        return KeyTransparency.Client(
            chatConnection: self,
            asyncContext: self.tokioAsyncContext,
            environment: self.environment
        )
    }
}
