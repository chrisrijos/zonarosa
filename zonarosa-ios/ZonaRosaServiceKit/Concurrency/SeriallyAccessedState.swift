//
// Copyright 2025 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

/// A wrapper around arbitrary state that asynchronously serializes access to
/// that state, allowing callers to perform async work while maintaining
/// exclusive access.
public class SeriallyAccessedState<State> {
    private var state: State
    private let updatesQueue: SerialTaskQueue

    public init(_ initialState: State) {
        self.state = initialState
        self.updatesQueue = SerialTaskQueue()
    }

    public func enqueueUpdate(_ update: @escaping (inout State) async -> Void) {
        updatesQueue.enqueue { [self] in
            await update(&state)
        }
    }
}
