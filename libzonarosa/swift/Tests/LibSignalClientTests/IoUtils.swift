//
// Copyright 2024 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import LibZonaRosaClient

internal struct TestIoError: Error {}

public class ErrorInputStream: ZonaRosaInputStream {
    public func read(into buffer: UnsafeMutableRawBufferPointer) throws -> Int {
        throw TestIoError()
    }

    public func skip(by amount: UInt64) throws {
        throw TestIoError()
    }
}

public class ThrowsAfterInputStream: ZonaRosaInputStream {
    public init(inner: ZonaRosaInputStream, readBeforeThrow: UInt64) {
        self.inner = inner
        self.readBeforeThrow = readBeforeThrow
    }

    public func read(into buffer: UnsafeMutableRawBufferPointer) throws -> Int {
        if self.readBeforeThrow == 0 {
            throw TestIoError()
        }

        var target = buffer
        if buffer.count > self.readBeforeThrow {
            target = UnsafeMutableRawBufferPointer(rebasing: buffer[..<Int(self.readBeforeThrow)])
        }

        let read = try inner.read(into: target)
        if read > 0 {
            self.readBeforeThrow -= UInt64(read)
        }
        return read
    }

    public func skip(by amount: UInt64) throws {
        if self.readBeforeThrow < amount {
            self.readBeforeThrow = 0
            throw TestIoError()
        }

        try self.inner.skip(by: amount)
        self.readBeforeThrow -= amount
    }

    private var inner: ZonaRosaInputStream
    private var readBeforeThrow: UInt64
}

#if !os(iOS) || targetEnvironment(simulator)

func readResource(forName name: String) -> Data {
    try! Data(
        contentsOf: URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()
            .appendingPathComponent("Resources")
            .appendingPathComponent(name)
    )
}

#endif
