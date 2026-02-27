//
// Copyright 2023 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import ZonaRosaFfi

/// An input stream of bytes.
///
/// This protocol is implemented for `FileHandle`.
public protocol ZonaRosaInputStream: AnyObject {
    /// Read an amount of bytes from the input stream.
    ///
    /// The actual amount of bytes returned may be smaller than the buffer provided by the caller, for any reason;
    /// however, reading zero bytes always indicates that the end of the stream has been reached.
    ///
    /// - Parameter buffer: The buffer to read the bytes into.
    /// - Returns: The actual number of bytes read.
    /// - Throws: If an I/O error occurred while reading from the input.
    func read(into buffer: UnsafeMutableRawBufferPointer) throws -> Int

    /// Skip an amount of bytes in the input stream.
    ///
    /// If the requested number of bytes could not be skipped for any reason, including if the end of stream was
    /// reached, an error must be raised.
    ///
    /// - Parameter amount: The amount of bytes to skip.
    /// - Throws:If an I/O error occurred while skipping the bytes in the input.
    func skip(by amount: UInt64) throws
}

/// An error thrown by `ZonaRosaInputStreamAdapter`.
public enum ZonaRosaInputStreamError: Error {
    /// The end of the input stream was reached while attempting to `skip()`.
    case unexpectedEof
}

extension FileHandle: ZonaRosaInputStream {
    public func read(into buffer: UnsafeMutableRawBufferPointer) throws -> Int {
        let data = self.readData(ofLength: buffer.count)
        return data.copyBytes(to: buffer)
    }

    public func skip(by amount: UInt64) throws {
        self.seek(toFileOffset: self.offsetInFile + amount)
    }
}

/// An adapter implementing `ZonaRosaInputStream` for any `Collection<UInt8>`.
public class ZonaRosaInputStreamAdapter<Inner>: ZonaRosaInputStream where Inner: Collection<UInt8> {
    var inner: Inner.SubSequence

    public init(_ inner: Inner) {
        self.inner = inner[...]
    }

    public func read(into buffer: UnsafeMutableRawBufferPointer) throws -> Int {
        let amount = min(buffer.count, self.inner.count)
        buffer.copyBytes(from: self.inner.prefix(amount))
        self.inner = self.inner.dropFirst(amount)
        return amount
    }

    public func skip(by amount: UInt64) throws {
        if amount > UInt64(self.inner.count) {
            throw ZonaRosaInputStreamError.unexpectedEof
        }
        self.inner = self.inner.dropFirst(Int(amount))
    }
}
