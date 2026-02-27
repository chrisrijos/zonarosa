//
// Copyright 2025 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import ZonaRosaFfi

public class RegistrationSessionState: NativeHandleOwner<ZonaRosaMutPointerRegistrationSession> {
    override internal class func destroyNativeHandle(
        _ nativeHandle: NonNull<ZonaRosaMutPointerRegistrationSession>
    ) -> ZonaRosaFfiErrorRef? {
        zonarosa_registration_session_destroy(nativeHandle.pointer)
    }

    public var allowedToRequestCode: Bool {
        return failOnError {
            try invokeFnReturningBool { out in
                self.withNativeHandle {
                    zonarosa_registration_session_get_allowed_to_request_code(out, $0.const())
                }
            }
        }
    }

    public var verified: Bool {
        return failOnError {
            try invokeFnReturningBool { out in
                self.withNativeHandle {
                    zonarosa_registration_session_get_verified(out, $0.const())
                }
            }
        }
    }

    public var nextSms: TimeInterval? {
        return failOnError {
            try invokeFnReturningOptionalInteger { out in
                self.withNativeHandle {
                    zonarosa_registration_session_get_next_sms_seconds(out, $0.const())
                }
            }.map { TimeInterval($0) }
        }
    }

    public var nextCall: TimeInterval? {
        return failOnError {
            try invokeFnReturningOptionalInteger { out in
                self.withNativeHandle {
                    zonarosa_registration_session_get_next_call_seconds(out, $0.const())
                }
            }.map { TimeInterval($0) }
        }
    }

    public var nextVerificationAttempt: TimeInterval? {
        return failOnError {
            try invokeFnReturningOptionalInteger { out in
                self.withNativeHandle {
                    zonarosa_registration_session_get_next_verification_attempt_seconds(out, $0.const())
                }
            }.map { TimeInterval($0) }
        }
    }

    public var requestedInformation: Set<ChallengeOption> {
        return failOnError {
            let items = try invokeFnReturningData { out in
                self.withNativeHandle {
                    zonarosa_registration_session_get_requested_information(out, $0.const())
                }
            }
            return Set(try items.map { try ChallengeOption(fromNative: $0) })
        }
    }
}

extension ChallengeOption {
    internal init(fromNative value: UInt8) throws {
        self =
            switch UInt32(value) {
            case ZonaRosaChallengeOptionCaptcha.rawValue:
                .captcha
            case ZonaRosaChallengeOptionPushChallenge.rawValue:
                .pushChallenge
            default:
                throw ZonaRosaError.internalError("unknown requested information")
            }
    }
}

public class RegisterAccountResponse: NativeHandleOwner<ZonaRosaMutPointerRegisterAccountResponse>, @unchecked Sendable {
    override internal class func destroyNativeHandle(
        _ nativeHandle: NonNull<ZonaRosaMutPointerRegisterAccountResponse>
    ) -> ZonaRosaFfiErrorRef? {
        zonarosa_register_account_response_destroy(nativeHandle.pointer)
    }

    public var aci: Aci {
        return failOnError {
            try self.withNativeHandle { native in
                try invokeFnReturningServiceId {
                    zonarosa_register_account_response_get_identity($0, native.const(), ServiceIdKind.aci.rawValue)
                }
            }
        }
    }

    public var pni: Pni {
        return failOnError {
            try self.withNativeHandle { native in
                try invokeFnReturningServiceId {
                    zonarosa_register_account_response_get_identity($0, native.const(), ServiceIdKind.pni.rawValue)
                }
            }
        }
    }

    public var number: String {
        return failOnError {
            try self.withNativeHandle { native in
                try invokeFnReturningString {
                    zonarosa_register_account_response_get_number($0, native.const())
                }
            }
        }
    }

    public var usernameHash: Data? {
        return failOnError {
            try self.withNativeHandle { native in
                try invokeFnReturningOptionalArray {
                    zonarosa_register_account_response_get_username_hash($0, native.const())
                }
            }
        }
    }

    public var usernameLinkHandle: UUID? {
        return failOnError {
            try self.withNativeHandle { native in
                try invokeFnReturningOptionalUuid {
                    zonarosa_register_account_response_get_username_link_handle($0, native.const())
                }
            }
        }
    }

    public var storageCapable: Bool {
        return failOnError {
            try self.withNativeHandle { native in
                try invokeFnReturningBool {
                    zonarosa_register_account_response_get_storage_capable($0, native.const())
                }
            }
        }
    }

    public var reregistration: Bool {
        return failOnError {
            try self.withNativeHandle { native in
                try invokeFnReturningBool {
                    zonarosa_register_account_response_get_reregistration($0, native.const())
                }
            }
        }
    }

    public var entitlements: ([BadgeEntitlement], BackupEntitlement?) {
        return failOnError {
            try self.withNativeHandle { native in
                let badges = try invokeFnReturningBadgeEntitlementArray {
                    zonarosa_register_account_response_get_entitlement_badges($0, native.const())
                }

                let backup = try BackupEntitlement(fromResponse: native.const())

                return (badges, backup)
            }
        }
    }
}

public struct BadgeEntitlement: Equatable {
    public let id: String
    public let visible: Bool
    public let expiration: TimeInterval

    public init(id: String, visible: Bool, expiration: TimeInterval) {
        self.id = id
        self.visible = visible
        self.expiration = expiration
    }
}

public struct BackupEntitlement: Equatable {
    public let expiration: TimeInterval
    public let level: UInt64

    public init(expiration: TimeInterval, level: UInt64) {
        self.expiration = expiration
        self.level = level
    }

    fileprivate init?(fromResponse native: ZonaRosaConstPointerRegisterAccountResponse) throws {
        let backupExpiration = try invokeFnReturningOptionalInteger {
            zonarosa_register_account_response_get_entitlement_backup_expiration_seconds($0, native)
        }
        guard case .some(let expiration) = backupExpiration else {
            return nil
        }

        let level = try invokeFnReturningOptionalInteger {
            zonarosa_register_account_response_get_entitlement_backup_level($0, native)
        }
        guard case .some(let level) = level else {
            return nil
        }
        self.init(expiration: TimeInterval(expiration), level: level)
    }
}

public enum VerificationTransport: CustomStringConvertible {
    case voice
    case sms

    public var description: String {
        return switch self {
        case .voice: "voice"
        case .sms: "sms"
        }
    }
}

public enum Svr2CredentialsResult {
    case match
    case noMatch
    case invalid
}

public enum ChallengeOption: Hashable, Sendable {
    case captcha
    case pushChallenge
}

/// Account attributes sent as part of a ``RegistrationService/registerAccount(accountPassword:skipDeviceTransfer:accountAttributes:apnPushToken:aciPublicKey:pniPublicKey:aciSignedPreKey:pniSignedPreKey:aciPqLastResortPreKey:pniPqLastResortPreKey:)`` request.
public class RegisterAccountAttributes: NativeHandleOwner<ZonaRosaMutPointerRegistrationAccountAttributes> {
    /// Constructs the set of attributes to pass to the server.
    /// - Throws: ``ZonaRosaError/invalidArgument(_:)`` if the `unidentifiedAccessKey` is not 16 bytes.
    public convenience init(
        recoveryPassword: Data,
        aciRegistrationId: UInt16,
        pniRegistrationId: UInt16,
        registrationLock: String?,
        unidentifiedAccessKey: Data,
        unrestrictedUnidentifiedAccess: Bool,
        capabilities: [String],
        discoverableByPhoneNumber: Bool
    ) throws {
        var uak = ZonaRosaUnidentifiedAccessKey(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        try withUnsafeMutableBytes(of: &uak) { uakBytes in
            if uakBytes.count != unidentifiedAccessKey.count {
                throw ZonaRosaError.invalidArgument(
                    "unidentifiedAccessKey has \(unidentifiedAccessKey.count) bytes; expected \(uakBytes.count)"
                )
            }
            uakBytes.copyBytes(from: unidentifiedAccessKey)
        }
        let nativeHandle = failOnError {
            try recoveryPassword.withUnsafeBorrowedBuffer { recoveryPassword in
                try registrationLock.withCString { registrationLock in
                    try withUnsafePointer(to: &uak) { unidentifiedAccessKey in
                        try capabilities.withUnsafeBorrowedBytestringArray { capabilities in
                            try invokeFnReturningValueByPointer(.init()) {
                                zonarosa_registration_account_attributes_create(
                                    $0,
                                    recoveryPassword,
                                    aciRegistrationId,
                                    pniRegistrationId,
                                    registrationLock,
                                    unidentifiedAccessKey,
                                    unrestrictedUnidentifiedAccess,
                                    capabilities,
                                    discoverableByPhoneNumber
                                )
                            }
                        }
                    }
                }
            }
        }
        self.init(owned: NonNull(nativeHandle)!)
    }

    override internal class func destroyNativeHandle(
        _ nativeHandle: NonNull<ZonaRosaMutPointerRegistrationAccountAttributes>
    ) -> ZonaRosaFfiErrorRef? {
        zonarosa_registration_account_attributes_destroy(nativeHandle.pointer)
    }
}

extension ZonaRosaFfiRegistrationCreateSessionRequest {
    internal static func withNativeStruct<Result>(
        e164: String,
        pushToken: String?,
        mcc: String?,
        mnc: String?,
        _ fn: (Self) throws -> Result
    ) rethrows -> Result {
        try e164.withCString { e164 in
            try pushToken.withCString { pushToken in
                try mcc.withCString { mcc in
                    try mnc.withCString { mnc in
                        let request = ZonaRosaFfiRegistrationCreateSessionRequest(
                            number: e164,
                            push_token: pushToken,
                            mcc: mcc,
                            mnc: mnc
                        )
                        return try fn(request)
                    }
                }
            }
        }
    }
}

/// Invoke a function returning an unsigned integral result where `nil` is bridged as the maximum value.
///
/// Bridging `nil` as max isn't a convention we want to rely on generally. It's
/// true for the getters in this file, though, hence `fileprivate`.
private func invokeFnReturningOptionalInteger<Result: FixedWidthInteger & UnsignedInteger>(
    fn: (UnsafeMutablePointer<Result>?) -> ZonaRosaFfiErrorRef?
) throws -> Result? {
    let output = try invokeFnReturningInteger(fn: fn)
    return if output == Result.max { nil } else { output }
}

private func invokeFnReturningBadgeEntitlementArray(
    fn: (_ out: UnsafeMutablePointer<ZonaRosaOwnedBufferOfFfiRegisterResponseBadge>?) -> ZonaRosaFfiErrorRef?
) throws -> [BadgeEntitlement] {
    let out = try invokeFnReturningValueByPointer(.init(), fn: fn)
    defer { zonarosa_free_list_of_register_response_badges(out) }

    return UnsafeBufferPointer(start: out.base, count: out.length).map {
        BadgeEntitlement(id: String(cString: $0.id), visible: $0.visible, expiration: TimeInterval($0.expiration_secs))
    }
}

extension ZonaRosaMutPointerRegistrationSession: ZonaRosaMutPointer {
    public typealias ConstPointer = ZonaRosaConstPointerRegistrationSession

    public init(untyped: OpaquePointer?) {
        self.init(raw: untyped)
    }

    public func toOpaque() -> OpaquePointer? {
        self.raw
    }

    public func const() -> Self.ConstPointer {
        ZonaRosaConstPointerRegistrationSession(raw: self.raw)
    }
}

extension ZonaRosaConstPointerRegistrationSession: ZonaRosaConstPointer {
    public func toOpaque() -> OpaquePointer? {
        self.raw
    }
}

extension ZonaRosaMutPointerRegisterAccountResponse: ZonaRosaMutPointer {
    public typealias ConstPointer = ZonaRosaConstPointerRegisterAccountResponse

    public init(untyped: OpaquePointer?) {
        self.init(raw: untyped)
    }

    public func toOpaque() -> OpaquePointer? {
        self.raw
    }

    public func const() -> Self.ConstPointer {
        ZonaRosaConstPointerRegisterAccountResponse(raw: self.raw)
    }
}

extension ZonaRosaConstPointerRegisterAccountResponse: ZonaRosaConstPointer {
    public func toOpaque() -> OpaquePointer? {
        self.raw
    }
}

extension ZonaRosaMutPointerRegisterAccountRequest: ZonaRosaMutPointer {
    public typealias ConstPointer = ZonaRosaConstPointerRegisterAccountRequest

    public init(untyped: OpaquePointer?) {
        self.init(raw: untyped)
    }

    public func toOpaque() -> OpaquePointer? {
        self.raw
    }

    public func const() -> Self.ConstPointer {
        ZonaRosaConstPointerRegisterAccountRequest(raw: self.raw)
    }
}

extension ZonaRosaConstPointerRegisterAccountRequest: ZonaRosaConstPointer {
    public func toOpaque() -> OpaquePointer? {
        self.raw
    }
}

extension ZonaRosaMutPointerRegistrationAccountAttributes: ZonaRosaMutPointer {
    public typealias ConstPointer = ZonaRosaConstPointerRegistrationAccountAttributes

    public init(untyped: OpaquePointer?) {
        self.init(raw: untyped)
    }

    public func toOpaque() -> OpaquePointer? {
        self.raw
    }

    public func const() -> Self.ConstPointer {
        ZonaRosaConstPointerRegistrationAccountAttributes(raw: self.raw)
    }
}

extension ZonaRosaConstPointerRegistrationAccountAttributes: ZonaRosaConstPointer {
    public func toOpaque() -> OpaquePointer? {
        self.raw
    }
}
