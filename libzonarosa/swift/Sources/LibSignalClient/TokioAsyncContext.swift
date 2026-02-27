//
// Copyright 2024 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import ZonaRosaFfi

internal class TokioAsyncContext: NativeHandleOwner<ZonaRosaMutPointerTokioAsyncContext>, @unchecked Sendable {
    convenience init() {
        let handle = failOnError {
            try invokeFnReturningValueByPointer(.init()) {
                zonarosa_tokio_async_context_new($0)
            }
        }
        self.init(owned: NonNull(handle)!)
    }

    override internal class func destroyNativeHandle(
        _ handle: NonNull<ZonaRosaMutPointerTokioAsyncContext>
    ) -> ZonaRosaFfiErrorRef? {
        zonarosa_tokio_async_context_destroy(handle.pointer)
    }

    /// A thread-safe helper for translating Swift task cancellations into calls to
    /// `zonarosa_tokio_async_context_cancel`.
    private final class CancellationHandoffHelper: @unchecked Sendable {
        // We'd like to remove the `@unchecked` above but Swift 5.10 still complains about
        // 'state' being mutable despite `nonisolated(unsafe)`.
        enum State {
            case initial
            case started(ZonaRosaCancellationId)
            case cancelled
        }

        // Emulates Rust's `Mutex<State>` (and the containing class is providing an `Arc`)
        // Unfortunately, doing this in Swift requires a separate allocation for the lock today.
        nonisolated(unsafe) var state: State = .initial
        let lock = NSLock()

        let context: TokioAsyncContext

        init(context: TokioAsyncContext) {
            self.context = context
        }

        func setCancellationId(_ id: ZonaRosaCancellationId) {
            // Ideally we would use NSLock.withLock here, but that's not available on Linux,
            // which we still support for development and CI.
            do {
                self.lock.lock()
                defer { self.lock.unlock() }

                switch self.state {
                case .initial:
                    self.state = .started(id)
                    fallthrough
                case .started(_):
                    return
                case .cancelled:
                    break
                }
            }

            // If we didn't early-exit, we're already cancelled.
            self.cancel(id)
        }

        func cancel() {
            let cancelId: ZonaRosaCancellationId
            // Ideally we would use NSLock.withLock here, but that's not available on Linux,
            // which we still support for development and CI.
            do {
                self.lock.lock()
                defer { self.lock.unlock() }

                defer { state = .cancelled }
                switch self.state {
                case .started(let id):
                    cancelId = id
                case .initial, .cancelled:
                    return
                }
            }

            // If we didn't early-exit, the task has already started and we need to cancel it.
            self.cancel(cancelId)
        }

        func cancel(_ id: ZonaRosaCancellationId) {
            do {
                try self.context.withNativeHandle {
                    try checkError(zonarosa_tokio_async_context_cancel($0.const(), id))
                }
            } catch {
                LoggerBridge.shared?.logger.log(
                    level: .warn,
                    file: #fileID,
                    line: #line,
                    message: "failed to cancel libzonarosa task \(id): \(error)"
                )
            }
        }
    }

    /// Provides a callback and context for calling Promise-based libzonarosa\_ffi functions, with cancellation supported.
    ///
    /// Example:
    ///
    /// ```
    /// let result = try await asyncContext.invokeAsyncFunction { promise, runtime in
    ///   zonarosa_do_async_work(promise, runtime, someInput, someOtherInput)
    /// }
    /// ```
    internal func invokeAsyncFunction<Promise: PromiseStruct>(
        _ body: (UnsafeMutablePointer<Promise>, ZonaRosaMutPointerTokioAsyncContext) -> ZonaRosaFfiErrorRef?
    ) async throws -> Promise.Result {
        let cancellationHelper = CancellationHandoffHelper(context: self)
        return try await withTaskCancellationHandler(
            operation: {
                try await LibZonaRosaClient.invokeAsyncFunction(
                    { promise in
                        withNativeHandle { handle in
                            body(promise, handle)
                        }
                    },
                    saveCancellationId: {
                        cancellationHelper.setCancellationId($0)
                    }
                )
            },
            onCancel: {
                cancellationHelper.cancel()
            }
        )
    }
}

extension ZonaRosaMutPointerTokioAsyncContext: ZonaRosaMutPointer {
    public typealias ConstPointer = ZonaRosaConstPointerTokioAsyncContext

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

extension ZonaRosaConstPointerTokioAsyncContext: ZonaRosaConstPointer {
    public func toOpaque() -> OpaquePointer? {
        self.raw
    }
}
