//
// Copyright 2023 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import ZonaRosaFfi

internal func withInputStream<Result>(
    _ stream: ZonaRosaInputStream,
    _ body: (ZonaRosaConstPointerFfiInputStreamStruct) throws -> Result
) throws -> Result {
    func ffiShimRead(
        stream_ctx: UnsafeMutableRawPointer?,
        pAmountRead: UnsafeMutablePointer<Int>?,
        buf: ZonaRosaBorrowedMutableBuffer,
    ) -> Int32 {
        let streamContext = stream_ctx!.assumingMemoryBound(to: ErrorHandlingContext<ZonaRosaInputStream>.self)
        return streamContext.pointee.catchCallbackErrors { stream in
            let buf = UnsafeMutableRawBufferPointer(start: buf.base, count: buf.length)
            let amountRead = try stream.read(into: buf)
            pAmountRead!.pointee = amountRead
        }
    }

    func ffiShimSkip(stream_ctx: UnsafeMutableRawPointer?, amount: UInt64) -> Int32 {
        let streamContext = stream_ctx!.assumingMemoryBound(to: ErrorHandlingContext<ZonaRosaInputStream>.self)
        return streamContext.pointee.catchCallbackErrors { stream in
            try stream.skip(by: amount)
        }
    }

    return try rethrowCallbackErrors(stream) {
        var ffiStream = ZonaRosaFfi.ZonaRosaInputStream(
            ctx: $0,
            read: ffiShimRead as ZonaRosaFfiBridgeInputStreamRead,
            skip: ffiShimSkip as ZonaRosaFfiBridgeInputStreamSkip,
            destroy: { _ in }
        )
        return try withUnsafePointer(to: &ffiStream) {
            try body(ZonaRosaConstPointerFfiInputStreamStruct(raw: $0))
        }
    }
}
