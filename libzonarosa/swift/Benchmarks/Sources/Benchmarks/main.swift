//
// Copyright 2024 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import Benchmark
import LibZonaRosaClient

Benchmark.main([
    groupSendEndorsementsSuite,
    privateKeyOperationsSuite,
    publicKeyOperationsSuite,
    hexSuite,
])

/// Attempts to prevent the value of `x` from being discarded by the optimizer.
///
/// See https://github.com/google/swift-benchmark/issues/69
@inline(__always)
internal func blackHole<T>(_ x: T) {
    @_optimize(none)
    func assumePointeeIsRead(_: UnsafeRawPointer) {}

    withUnsafePointer(to: x) { assumePointeeIsRead($0) }
}
