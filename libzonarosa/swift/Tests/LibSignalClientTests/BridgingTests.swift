//
// Copyright 2023 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

// These testing endpoints aren't generated in device builds, to save on code size.
#if !os(iOS) || targetEnvironment(simulator)

@testable import LibZonaRosaClient
import ZonaRosaFfi
import XCTest

private func fakeAsyncRuntime() -> ZonaRosaConstPointerNonSuspendingBackgroundThreadRuntime {
    ZonaRosaConstPointerNonSuspendingBackgroundThreadRuntime(raw: OpaquePointer(bitPattern: -1))
}

private func invokeFnIgnoringResult<T>(fn: (UnsafeMutablePointer<T>?) -> ZonaRosaFfiErrorRef?) throws {
    // Swift doesn't have a way to declare uninitialized local variables, so we have to allocate one on the heap.
    // This will almost certainly get stack-promoted, but since this is a test it doesn't really matter anyway.
    let output = UnsafeMutablePointer<T>.allocate(capacity: 1)
    defer { output.deallocate() }
    try checkError(fn(output))
}

final class BridgingTests: XCTestCase {
    func testErrorOnBorrow() async throws {
        do {
            try checkError(zonarosa_testing_error_on_borrow_sync(nil))
            XCTFail("should have failed")
        } catch ZonaRosaError.invalidArgument(_) {
            // good
        }

        do {
            try checkError(zonarosa_testing_error_on_borrow_async(nil))
            XCTFail("should have failed")
        } catch ZonaRosaError.invalidArgument(_) {
            // good
        }

        do {
            _ = try await invokeAsyncFunction {
                zonarosa_testing_error_on_borrow_io($0, fakeAsyncRuntime(), nil)
            }
            XCTFail("should have failed")
        } catch ZonaRosaError.invalidArgument(_) {
            // good
        }
    }

    func testPanicOnBorrow() async throws {
        do {
            try checkError(zonarosa_testing_panic_on_borrow_sync(nil))
            XCTFail("should have failed")
        } catch ZonaRosaError.internalError(_) {
            // good
        }

        do {
            try checkError(zonarosa_testing_panic_on_borrow_async(nil))
            XCTFail("should have failed")
        } catch ZonaRosaError.internalError(_) {
            // good
        }

        do {
            _ = try await invokeAsyncFunction {
                zonarosa_testing_panic_on_borrow_io($0, fakeAsyncRuntime(), nil)
            }
            XCTFail("should have failed")
        } catch ZonaRosaError.internalError(_) {
            // good
        }
    }

    func testPanicOnLoad() async throws {
        do {
            try checkError(zonarosa_testing_panic_on_load_sync(nil, nil))
            XCTFail("should have failed")
        } catch ZonaRosaError.internalError(_) {
            // good
        }

        do {
            try checkError(zonarosa_testing_panic_on_load_async(nil, nil))
            XCTFail("should have failed")
        } catch ZonaRosaError.internalError(_) {
            // good
        }

        do {
            _ = try await invokeAsyncFunction {
                zonarosa_testing_panic_on_load_io($0, fakeAsyncRuntime(), nil, nil)
            }
            XCTFail("should have failed")
        } catch ZonaRosaError.internalError(_) {
            // good
        }
    }

    func testPanicInBody() async throws {
        do {
            try checkError(zonarosa_testing_panic_in_body_sync(nil))
            XCTFail("should have failed")
        } catch ZonaRosaError.internalError(_) {
            // good
        }

        do {
            try checkError(zonarosa_testing_panic_in_body_async(nil))
            XCTFail("should have failed")
        } catch ZonaRosaError.internalError(_) {
            // good
        }

        do {
            _ = try await invokeAsyncFunction {
                zonarosa_testing_panic_in_body_io($0, fakeAsyncRuntime(), nil)
            }
            XCTFail("should have failed")
        } catch ZonaRosaError.internalError(_) {
            // good
        }
    }

    func testErrorOnReturn() async throws {
        do {
            try invokeFnIgnoringResult { zonarosa_testing_error_on_return_sync($0, nil) }
            XCTFail("should have failed")
        } catch ZonaRosaError.invalidArgument(_) {
            // good
        }

        do {
            try invokeFnIgnoringResult { zonarosa_testing_error_on_return_async($0, nil) }
            XCTFail("should have failed")
        } catch ZonaRosaError.invalidArgument(_) {
            // good
        }

        do {
            _ = try await invokeAsyncFunction {
                zonarosa_testing_error_on_return_io($0, fakeAsyncRuntime(), nil)
            }
            XCTFail("should have failed")
        } catch ZonaRosaError.invalidArgument(_) {
            // good
        }
    }

    func testPanicOnReturn() async throws {
        do {
            try invokeFnIgnoringResult { zonarosa_testing_panic_on_return_sync($0, nil) }
            XCTFail("should have failed")
        } catch ZonaRosaError.internalError(_) {
            // good
        }

        do {
            try invokeFnIgnoringResult { zonarosa_testing_panic_on_return_async($0, nil) }
            XCTFail("should have failed")
        } catch ZonaRosaError.internalError(_) {
            // good
        }

        do {
            _ = try await invokeAsyncFunction {
                zonarosa_testing_panic_on_return_io($0, fakeAsyncRuntime(), nil)
            }
            XCTFail("should have failed")
        } catch ZonaRosaError.internalError(_) {
            // good
        }
    }

    func testReturnStringArray() throws {
        let EXPECTED = ["easy", "as", "ABC", "123"]
        let array = try invokeFnReturningStringArray {
            zonarosa_testing_return_string_array($0)
        }
        XCTAssertEqual(array, EXPECTED)
    }

    func testBytestringArray() throws {
        let first: [UInt8] = [1, 2, 3]
        let empty: [UInt8] = []
        let second: [UInt8] = [4, 5, 6]
        let result = try first.withUnsafeBytes { first in
            try empty.withUnsafeBytes { empty in
                try second.withUnsafeBytes { second in
                    let slices = [
                        ZonaRosaBorrowedBuffer(first), ZonaRosaBorrowedBuffer(empty), ZonaRosaBorrowedBuffer(second),
                    ]
                    return try slices.withUnsafeBufferPointer { slices in
                        try invokeFnReturningBytestringArray {
                            zonarosa_testing_process_bytestring_array(
                                $0,
                                ZonaRosaBorrowedSliceOfBuffers(base: slices.baseAddress, length: slices.count)
                            )
                        }
                    }
                }
            }
        }
        XCTAssertEqual(result, [[1, 2, 3, 1, 2, 3], [], [4, 5, 6, 4, 5, 6]].map { Data($0) })
    }

    func testBytestringArrayEmpty() throws {
        let slices: [ZonaRosaBorrowedBuffer] = []
        let result = try slices.withUnsafeBufferPointer { slices in
            try invokeFnReturningBytestringArray {
                zonarosa_testing_process_bytestring_array(
                    $0,
                    ZonaRosaBorrowedSliceOfBuffers(base: slices.baseAddress, length: slices.count)
                )
            }
        }
        XCTAssertEqual(result, [])
    }

    func testBridgedStringMap() throws {
        let empty = try [:].withBridgedStringMap { map in
            try invokeFnReturningString {
                zonarosa_testing_bridged_string_map_dump_to_json($0, map.const())
            }
        }
        XCTAssertEqual(empty, "{}")

        let dumped = try ["b": "bbb", "a": "aaa", "c": "ccc"].withBridgedStringMap { map in
            try invokeFnReturningString {
                zonarosa_testing_bridged_string_map_dump_to_json($0, map.const())
            }
        }
        XCTAssertEqual(
            dumped,
            """
            {
              "a": "aaa",
              "b": "bbb",
              "c": "ccc"
            }
            """
        )
    }

    func testReturnOptionalUuid() throws {
        let shouldBeNil = try invokeFnReturningOptionalUuid {
            zonarosa_testing_convert_optional_uuid($0, false)
        }
        XCTAssertEqual(nil, shouldBeNil)
        let shouldBePresent = try invokeFnReturningOptionalUuid {
            zonarosa_testing_convert_optional_uuid($0, true)
        }
        XCTAssertEqual(UUID(uuidString: "abababab-1212-8989-baba-565656565656"), shouldBePresent)
    }

    func testFingerprintVersionMismatchError() throws {
        let theirs = UInt32(11)
        let ours = UInt32(22)
        do {
            try checkError(zonarosa_testing_fingerprint_version_mismatch_error(theirs, ours))
            XCTFail("should have thrown")
        } catch ZonaRosaError.fingerprintVersionMismatch(let actualTheirs, let actualOurs) {
            XCTAssertEqual(theirs, actualTheirs)
            XCTAssertEqual(ours, actualOurs)
        }
    }

    func testReturnPair() throws {
        let pair = try invokeFnReturningValueByPointer(.init()) {
            zonarosa_testing_return_pair($0)
        }
        defer { zonarosa_free_string(pair.second) }
        XCTAssertEqual(pair.first, 1 as Int32)
        XCTAssertEqual(String(cString: pair.second), "libzonarosa")
    }
}

#endif
