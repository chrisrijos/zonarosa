//
// Copyright 2024 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import ZonaRosaFfi

public struct AciAndAccessKey: Sendable {
    public let aci: Aci
    public let accessKey: Data
    public init(aci: Aci, accessKey: Data) {
        self.aci = aci
        self.accessKey = accessKey
    }
}

/// Information passed to the CDSI server when making a request.
public class CdsiLookupRequest: NativeHandleOwner<ZonaRosaMutPointerLookupRequest> {
    /// Indicates whether this request object was constructed with a token.
    public private(set) var hasToken: Bool = false

    private convenience init() {
        let handle = failOnError {
            try invokeFnReturningValueByPointer(.init()) {
                zonarosa_lookup_request_new($0)
            }
        }
        self.init(owned: NonNull(handle)!)
    }

    /// Creates a new `CdsiLookupRequest` with the provided data.
    ///
    /// Phone numbers should be passed in as string-encoded numeric values,
    /// optionally with a leading `+` character.
    ///
    /// - Throws: a ``ZonaRosaError`` if any of the arguments are invalid,
    /// including the phone numbers or the access keys.
    public convenience init(
        e164s: [String],
        prevE164s: [String],
        acisAndAccessKeys: [AciAndAccessKey],
        token: Data?
    ) throws {
        self.init()
        try self.withNativeHandle { handle in
            for e164 in e164s {
                try checkError(zonarosa_lookup_request_add_e164(handle.const(), e164))
            }

            for prevE164 in prevE164s {
                try checkError(zonarosa_lookup_request_add_previous_e164(handle.const(), prevE164))
            }

            for aciAndAccessKey in acisAndAccessKeys {
                let aci = aciAndAccessKey.aci
                let accessKey = aciAndAccessKey.accessKey
                try aci.withPointerToFixedWidthBinary { aci in
                    try accessKey.withUnsafeBorrowedBuffer { accessKey in
                        try checkError(zonarosa_lookup_request_add_aci_and_access_key(handle.const(), aci, accessKey))
                    }
                }
            }

            if let token = token {
                try token.withUnsafeBorrowedBuffer { token in
                    try checkError(zonarosa_lookup_request_set_token(handle.const(), token))
                }
                self.hasToken = true
            }
        }
    }

    override internal class func destroyNativeHandle(
        _ handle: NonNull<ZonaRosaMutPointerLookupRequest>
    ) -> ZonaRosaFfiErrorRef? {
        zonarosa_lookup_request_destroy(handle.pointer)
    }
}

extension ZonaRosaMutPointerLookupRequest: ZonaRosaMutPointer {
    public typealias ConstPointer = ZonaRosaConstPointerLookupRequest

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

extension ZonaRosaConstPointerLookupRequest: ZonaRosaConstPointer {
    public func toOpaque() -> OpaquePointer? {
        self.raw
    }
}

/// CDSI lookup in progress.
///
/// Returned by ``Net/cdsiLookup(auth:request:)`` when a request is successfully initiated.
public class CdsiLookup {
    class NativeCdsiLookup: NativeHandleOwner<ZonaRosaMutPointerCdsiLookup> {
        override internal class func destroyNativeHandle(
            _ handle: NonNull<ZonaRosaMutPointerCdsiLookup>
        ) -> ZonaRosaFfiErrorRef? {
            zonarosa_cdsi_lookup_destroy(handle.pointer)
        }
    }

    private var asyncContext: TokioAsyncContext
    private var native: NativeCdsiLookup

    internal init(native: NonNull<ZonaRosaMutPointerCdsiLookup>, asyncContext: TokioAsyncContext) {
        self.native = NativeCdsiLookup(owned: native)
        self.asyncContext = asyncContext
    }

    /// The token returned by the CDSI server.
    ///
    /// Clients can save this and pass it with future request to avoid getting
    /// "charged" for rate-limiting purposes for lookups of the same phone
    /// numbers.
    public var token: Data {
        failOnError {
            try self.native.withNativeHandle { handle in
                try invokeFnReturningData {
                    zonarosa_cdsi_lookup_token($0, handle.const())
                }
            }
        }
    }

    /// Asynchronously waits for the request to complete and returns the response.
    ///
    /// After this method is called on a ``CdsiLookup`` object, the object
    /// should not be used again.
    ///
    /// - Returns: The collected data from the server.
    ///
    /// - Throws: ``ZonaRosaError`` if the request fails for any reason, including
    ///   `ZonaRosaError.networkError` for a network-level connectivity issue,
    ///   `ZonaRosaError.networkProtocolError` for a CDSI or attested connection protocol issue.
    public func complete() async throws -> CdsiLookupResponse {
        let response: ZonaRosaFfiCdsiLookupResponse = try await self.asyncContext.invokeAsyncFunction {
            promise,
            asyncContext in
            self.native.withNativeHandle { handle in
                zonarosa_cdsi_lookup_complete(promise, asyncContext.const(), handle.const())
            }
        }

        return CdsiLookupResponse(
            entries: LookupResponseEntryList(owned: response.entries),
            debugPermitsUsed: response.debug_permits_used
        )
    }
}

extension ZonaRosaMutPointerCdsiLookup: ZonaRosaMutPointer {
    public typealias ConstPointer = ZonaRosaConstPointerCdsiLookup

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

extension ZonaRosaConstPointerCdsiLookup: ZonaRosaConstPointer {
    public func toOpaque() -> OpaquePointer? {
        self.raw
    }
}

/// Response to the server produced by a completed ``CdsiLookup``.
///
/// Returned by ``CdsiLookup/complete()`` on success.
public struct CdsiLookupResponse {
    // swiftlint:disable:previous explicit_init_for_public_struct

    /// The entries received from the server.
    public let entries: LookupResponseEntryList
    /// How many "permits" were used in making the request.
    public let debugPermitsUsed: Int32
}

/// Entries received from the CDSI server in response to a lookup request.
///
/// Contains a sequence of ``CdsiLookupResponseEntry`` values. Conforms
/// to the `Collection` protocol to allow indexing and iteration over those
/// values.
public class LookupResponseEntryList: Collection {
    private var owned: UnsafeMutableBufferPointer<CdsiLookupResponseEntry>

    init(owned: ZonaRosaOwnedBufferOfFfiCdsiLookupResponseEntry) {
        self.owned = UnsafeMutableBufferPointer(start: owned.base, count: Int(owned.length))
    }

    deinit {
        zonarosa_free_lookup_response_entry_list(
            ZonaRosaOwnedBufferOfFfiCdsiLookupResponseEntry(base: self.owned.baseAddress, length: self.owned.count)
        )
    }

    public typealias Index = UnsafeMutableBufferPointer<CdsiLookupResponseEntry>.Index
    public typealias Element = UnsafeMutableBufferPointer<CdsiLookupResponseEntry>.Element
    public typealias SubSequence = UnsafeMutableBufferPointer<CdsiLookupResponseEntry>.SubSequence

    public var startIndex: Index { self.owned.startIndex }

    public var endIndex: Index { self.owned.endIndex }

    public func index(after: Index) -> Index {
        self.owned.index(after: after)
    }

    public subscript(position: Index) -> Element { self.owned[position] }
    public subscript(bounds: Range<Index>) -> SubSequence { self.owned[bounds] }
}

let nilUuid = uuid_t(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)

/// Entry contained in a successful CDSI lookup response.
///
/// See ``CdsiLookupResponseEntryProtocol`` (which this type conforms to) for
/// getters for the various fields.
public typealias CdsiLookupResponseEntry = ZonaRosaFfiCdsiLookupResponseEntry

/// Getters for an entry in a CDSI lookup response.
public protocol CdsiLookupResponseEntryProtocol {
    /// The ACI in the response, if there was any.
    var aci: Aci? { get }
    /// The PNI in the response, if there was any.
    var pni: Pni? { get }
    /// The unformatted phone number for the entry.
    var e164: UInt64 { get }
}

extension CdsiLookupResponseEntry: CdsiLookupResponseEntryProtocol {
    public var aci: Aci? {
        let aciUuid = UUID(uuid: self.rawAciUuid)
        return aciUuid != UUID(uuid: nilUuid) ? Aci(fromUUID: aciUuid) : nil
    }

    public var pni: Pni? {
        let pniUuid = UUID(uuid: self.rawPniUuid)
        return pniUuid != UUID(uuid: nilUuid) ? Pni(fromUUID: pniUuid) : nil
    }

    init(e164: UInt64, _ aci: Aci?, _ pni: Pni?) {
        self.init(
            e164: e164,
            rawAciUuid: aci?.rawUUID.uuid ?? nilUuid,
            rawPniUuid: pni?.rawUUID.uuid ?? nilUuid
        )
    }
}

extension CdsiLookupResponseEntry: Swift.Equatable {
    public static func == (lhs: Self, rhs: Self) -> Bool {
        lhs.aci == rhs.aci && lhs.pni == rhs.pni && lhs.e164 == rhs.e164
    }
}
