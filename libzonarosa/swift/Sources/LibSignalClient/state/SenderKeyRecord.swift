//
// Copyright 2020-2022 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import ZonaRosaFfi

public class SenderKeyRecord: ClonableHandleOwner<ZonaRosaMutPointerSenderKeyRecord> {
    override internal class func destroyNativeHandle(
        _ handle: NonNull<ZonaRosaMutPointerSenderKeyRecord>
    ) -> ZonaRosaFfiErrorRef? {
        return zonarosa_sender_key_record_destroy(handle.pointer)
    }

    override internal class func cloneNativeHandle(
        _ newHandle: inout ZonaRosaMutPointerSenderKeyRecord,
        currentHandle: ZonaRosaConstPointerSenderKeyRecord
    ) -> ZonaRosaFfiErrorRef? {
        return zonarosa_sender_key_record_clone(&newHandle, currentHandle)
    }

    public convenience init<Bytes: ContiguousBytes>(bytes: Bytes) throws {
        let handle = try bytes.withUnsafeBorrowedBuffer { bytes in
            try invokeFnReturningValueByPointer(.init()) {
                zonarosa_sender_key_record_deserialize($0, bytes)
            }
        }
        self.init(owned: NonNull(handle)!)
    }

    public func serialize() -> Data {
        return withNativeHandle { nativeHandle in
            failOnError {
                try invokeFnReturningData {
                    zonarosa_sender_key_record_serialize($0, nativeHandle.const())
                }
            }
        }
    }
}

extension ZonaRosaMutPointerSenderKeyRecord: ZonaRosaMutPointer {
    public typealias ConstPointer = ZonaRosaConstPointerSenderKeyRecord

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

extension ZonaRosaConstPointerSenderKeyRecord: ZonaRosaConstPointer {
    public func toOpaque() -> OpaquePointer? {
        self.raw
    }
}
