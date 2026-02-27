//
// Copyright 2020-2021 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import ZonaRosaFfi

public enum ZonaRosaError: Error {
    case invalidState(String)
    case internalError(String)
    case nullParameter(String)
    case invalidArgument(String)
    case invalidType(String)
    case invalidUtf8String(String)
    case protobufError(String)
    case legacyCiphertextVersion(String)
    case unknownCiphertextVersion(String)
    case unrecognizedMessageVersion(String)
    case invalidMessage(String)
    case invalidKey(String)
    case invalidSignature(String)
    case invalidAttestationData(String)
    case fingerprintVersionMismatch(theirs: UInt32, ours: UInt32)
    case fingerprintParsingError(String)
    case sealedSenderSelfSend(String)
    case untrustedIdentity(String)
    case invalidKeyIdentifier(String)
    case sessionNotFound(String)
    case invalidSession(String)
    case invalidRegistrationId(address: ProtocolAddress, message: String)
    case invalidProtocolAddress(name: String, deviceId: UInt32, message: String)
    case invalidSenderKeySession(distributionId: UUID, message: String)
    case duplicatedMessage(String)
    case verificationFailed(String)
    case nicknameCannotBeEmpty(String)
    case nicknameCannotStartWithDigit(String)
    case missingSeparator(String)
    case badDiscriminatorCharacter(String)
    case badNicknameCharacter(String)
    case nicknameTooShort(String)
    case nicknameTooLong(String)
    case usernameLinkInvalidEntropyDataLength(String)
    case usernameLinkInvalid(String)
    case usernameDiscriminatorCannotBeEmpty(String)
    case usernameDiscriminatorCannotBeZero(String)
    case usernameDiscriminatorCannotBeSingleDigit(String)
    case usernameDiscriminatorCannotHaveLeadingZeros(String)
    case usernameDiscriminatorTooLarge(String)
    case ioError(String)
    case invalidMediaInput(String)
    case unsupportedMediaInput(String)
    case callbackError(String)
    case webSocketError(String)
    case connectionTimeoutError(String)
    case requestTimeoutError(String)
    case connectionFailed(String)
    case networkProtocolError(String)
    case cdsiInvalidToken(String)
    case rateLimitedError(retryAfter: TimeInterval, message: String)
    case rateLimitChallengeError(token: String, options: Set<ChallengeOption>, message: String)
    case svrDataMissing(String)
    case svrRestoreFailed(triesRemaining: UInt32, message: String)
    case svrRotationMachineTooManySteps(String)
    case svrRequestFailed(String)
    case chatServiceInactive(String)
    case appExpired(String)
    case deviceDeregistered(String)
    case connectionInvalidated(String)
    case connectedElsewhere(String)
    case possibleCaptiveNetwork(String)
    case keyTransparencyError(String)
    case keyTransparencyVerificationFailed(String)
    case requestUnauthorized(String)
    case mismatchedDevices(entries: [MismatchedDeviceEntry], message: String)

    case unknown(UInt32, String)
}

internal typealias ZonaRosaFfiErrorRef = OpaquePointer

internal func convertError(_ error: ZonaRosaFfiErrorRef?) -> Error? {
    // It would be *slightly* more efficient for checkError to call convertError,
    // instead of the other way around. However, then it would be harder to implement
    // checkError, since some of the conversion operations can themselves throw.
    // So this is more maintainable.
    do {
        try checkError(error)
        return nil
    } catch let thrownError {
        return thrownError
    }
}

internal func checkError(_ error: ZonaRosaFfiErrorRef?) throws {
    guard let error = error else { return }

    let errType = zonarosa_error_get_type(error)
    // If this actually throws we'd have an infinite loop before we hit the 'try!'.
    let errStr = try! invokeFnReturningString {
        zonarosa_error_get_message($0, error)
    }
    defer { zonarosa_error_free(error) }

    switch ZonaRosaErrorCode(errType) {
    case ZonaRosaErrorCodeCancelled:
        // Special case: don't use ZonaRosaError for this one.
        throw CancellationError()
    case ZonaRosaErrorCodeInvalidState:
        throw ZonaRosaError.invalidState(errStr)
    case ZonaRosaErrorCodeInternalError:
        throw ZonaRosaError.internalError(errStr)
    case ZonaRosaErrorCodeNullParameter:
        throw ZonaRosaError.nullParameter(errStr)
    case ZonaRosaErrorCodeInvalidArgument:
        throw ZonaRosaError.invalidArgument(errStr)
    case ZonaRosaErrorCodeInvalidType:
        throw ZonaRosaError.invalidType(errStr)
    case ZonaRosaErrorCodeInvalidUtf8String:
        throw ZonaRosaError.invalidUtf8String(errStr)
    case ZonaRosaErrorCodeProtobufError:
        throw ZonaRosaError.protobufError(errStr)
    case ZonaRosaErrorCodeLegacyCiphertextVersion:
        throw ZonaRosaError.legacyCiphertextVersion(errStr)
    case ZonaRosaErrorCodeUnknownCiphertextVersion:
        throw ZonaRosaError.unknownCiphertextVersion(errStr)
    case ZonaRosaErrorCodeUnrecognizedMessageVersion:
        throw ZonaRosaError.unrecognizedMessageVersion(errStr)
    case ZonaRosaErrorCodeInvalidMessage:
        throw ZonaRosaError.invalidMessage(errStr)
    case ZonaRosaErrorCodeFingerprintParsingError:
        throw ZonaRosaError.fingerprintParsingError(errStr)
    case ZonaRosaErrorCodeSealedSenderSelfSend:
        throw ZonaRosaError.sealedSenderSelfSend(errStr)
    case ZonaRosaErrorCodeInvalidKey:
        throw ZonaRosaError.invalidKey(errStr)
    case ZonaRosaErrorCodeInvalidSignature:
        throw ZonaRosaError.invalidSignature(errStr)
    case ZonaRosaErrorCodeInvalidAttestationData:
        throw ZonaRosaError.invalidAttestationData(errStr)
    case ZonaRosaErrorCodeFingerprintVersionMismatch:
        let theirs = try invokeFnReturningInteger {
            zonarosa_error_get_their_fingerprint_version($0, error)
        }
        let ours = try invokeFnReturningInteger {
            zonarosa_error_get_our_fingerprint_version($0, error)
        }
        throw ZonaRosaError.fingerprintVersionMismatch(theirs: theirs, ours: ours)
    case ZonaRosaErrorCodeUntrustedIdentity:
        throw ZonaRosaError.untrustedIdentity(errStr)
    case ZonaRosaErrorCodeInvalidKeyIdentifier:
        throw ZonaRosaError.invalidKeyIdentifier(errStr)
    case ZonaRosaErrorCodeSessionNotFound:
        throw ZonaRosaError.sessionNotFound(errStr)
    case ZonaRosaErrorCodeInvalidSession:
        throw ZonaRosaError.invalidSession(errStr)
    case ZonaRosaErrorCodeInvalidRegistrationId:
        let address: ProtocolAddress = try invokeFnReturningNativeHandle {
            zonarosa_error_get_address($0, error)
        }
        throw ZonaRosaError.invalidRegistrationId(address: address, message: errStr)
    case ZonaRosaErrorCodeInvalidProtocolAddress:
        let pair = try invokeFnReturningValueByPointer(.init()) {
            zonarosa_error_get_invalid_protocol_address($0, error)
        }
        defer { zonarosa_free_string(pair.first) }
        let name = String(cString: pair.first!)
        throw ZonaRosaError.invalidProtocolAddress(name: name, deviceId: pair.second, message: errStr)
    case ZonaRosaErrorCodeInvalidSenderKeySession:
        let distributionId = try invokeFnReturningUuid {
            zonarosa_error_get_uuid($0, error)
        }
        throw ZonaRosaError.invalidSenderKeySession(distributionId: distributionId, message: errStr)
    case ZonaRosaErrorCodeDuplicatedMessage:
        throw ZonaRosaError.duplicatedMessage(errStr)
    case ZonaRosaErrorCodeVerificationFailure:
        throw ZonaRosaError.verificationFailed(errStr)
    case ZonaRosaErrorCodeUsernameCannotBeEmpty:
        throw ZonaRosaError.nicknameCannotBeEmpty(errStr)
    case ZonaRosaErrorCodeUsernameCannotStartWithDigit:
        throw ZonaRosaError.nicknameCannotStartWithDigit(errStr)
    case ZonaRosaErrorCodeUsernameMissingSeparator:
        throw ZonaRosaError.missingSeparator(errStr)
    case ZonaRosaErrorCodeUsernameBadDiscriminatorCharacter:
        throw ZonaRosaError.badDiscriminatorCharacter(errStr)
    case ZonaRosaErrorCodeUsernameBadNicknameCharacter:
        throw ZonaRosaError.badNicknameCharacter(errStr)
    case ZonaRosaErrorCodeUsernameTooShort:
        throw ZonaRosaError.nicknameTooShort(errStr)
    case ZonaRosaErrorCodeUsernameTooLong:
        throw ZonaRosaError.nicknameTooLong(errStr)
    case ZonaRosaErrorCodeUsernameDiscriminatorCannotBeEmpty:
        throw ZonaRosaError.usernameDiscriminatorCannotBeEmpty(errStr)
    case ZonaRosaErrorCodeUsernameDiscriminatorCannotBeZero:
        throw ZonaRosaError.usernameDiscriminatorCannotBeZero(errStr)
    case ZonaRosaErrorCodeUsernameDiscriminatorCannotBeSingleDigit:
        throw ZonaRosaError.usernameDiscriminatorCannotBeSingleDigit(errStr)
    case ZonaRosaErrorCodeUsernameDiscriminatorCannotHaveLeadingZeros:
        throw ZonaRosaError.usernameDiscriminatorCannotHaveLeadingZeros(errStr)
    case ZonaRosaErrorCodeUsernameDiscriminatorTooLarge:
        throw ZonaRosaError.usernameDiscriminatorTooLarge(errStr)
    case ZonaRosaErrorCodeUsernameLinkInvalidEntropyDataLength:
        throw ZonaRosaError.usernameLinkInvalidEntropyDataLength(errStr)
    case ZonaRosaErrorCodeUsernameLinkInvalid:
        throw ZonaRosaError.usernameLinkInvalid(errStr)
    case ZonaRosaErrorCodeIoError:
        throw ZonaRosaError.ioError(errStr)
    case ZonaRosaErrorCodeInvalidMediaInput:
        throw ZonaRosaError.invalidMediaInput(errStr)
    case ZonaRosaErrorCodeUnsupportedMediaInput:
        throw ZonaRosaError.unsupportedMediaInput(errStr)
    case ZonaRosaErrorCodeCallbackError:
        throw ZonaRosaError.callbackError(errStr)
    case ZonaRosaErrorCodeWebSocket:
        throw ZonaRosaError.webSocketError(errStr)
    case ZonaRosaErrorCodeConnectionTimedOut:
        throw ZonaRosaError.connectionTimeoutError(errStr)
    case ZonaRosaErrorCodeRequestTimedOut:
        throw ZonaRosaError.requestTimeoutError(errStr)
    case ZonaRosaErrorCodeConnectionFailed:
        throw ZonaRosaError.connectionFailed(errStr)
    case ZonaRosaErrorCodeNetworkProtocol:
        throw ZonaRosaError.networkProtocolError(errStr)
    case ZonaRosaErrorCodeCdsiInvalidToken:
        throw ZonaRosaError.cdsiInvalidToken(errStr)
    case ZonaRosaErrorCodeRateLimited:
        let retryAfterSeconds = try invokeFnReturningInteger {
            zonarosa_error_get_retry_after_seconds($0, error)
        }
        throw ZonaRosaError.rateLimitedError(retryAfter: TimeInterval(retryAfterSeconds), message: errStr)
    case ZonaRosaErrorCodeRateLimitChallenge:
        let pair = try invokeFnReturningValueByPointer(.init()) {
            zonarosa_error_get_rate_limit_challenge($0, error)
        }
        defer {
            zonarosa_free_string(pair.first)
            zonarosa_free_buffer(pair.second.base, pair.second.length)
        }
        let token = String(cString: pair.first)
        let options = UnsafeBufferPointer(start: pair.second.base, count: pair.second.length)
        throw ZonaRosaError.rateLimitChallengeError(
            token: token,
            options: Set(try options.lazy.map { try ChallengeOption(fromNative: $0) }),
            message: errStr
        )
    case ZonaRosaErrorCodeSvrDataMissing:
        throw ZonaRosaError.svrDataMissing(errStr)
    case ZonaRosaErrorCodeSvrRestoreFailed:
        let triesRemaining = try invokeFnReturningInteger {
            zonarosa_error_get_tries_remaining($0, error)
        }
        throw ZonaRosaError.svrRestoreFailed(triesRemaining: triesRemaining, message: errStr)
    case ZonaRosaErrorCodeSvrRotationMachineTooManySteps:
        throw ZonaRosaError.svrRotationMachineTooManySteps(errStr)
    case ZonaRosaErrorCodeSvrRequestFailed:
        throw ZonaRosaError.svrRequestFailed(errStr)
    case ZonaRosaErrorCodeChatServiceInactive:
        throw ZonaRosaError.chatServiceInactive(errStr)
    case ZonaRosaErrorCodeAppExpired:
        throw ZonaRosaError.appExpired(errStr)
    case ZonaRosaErrorCodeDeviceDeregistered:
        throw ZonaRosaError.deviceDeregistered(errStr)
    case ZonaRosaErrorCodeConnectionInvalidated:
        throw ZonaRosaError.connectionInvalidated(errStr)
    case ZonaRosaErrorCodeConnectedElsewhere:
        throw ZonaRosaError.connectedElsewhere(errStr)
    case ZonaRosaErrorCodePossibleCaptiveNetwork:
        throw ZonaRosaError.possibleCaptiveNetwork(errStr)
    case ZonaRosaErrorCodeBackupValidation:
        let unknownFields = try invokeFnReturningStringArray {
            zonarosa_error_get_unknown_fields($0, error)
        }
        // Special case: we have a dedicated type for this one.
        throw MessageBackupValidationError(
            errorMessage: errStr,
            unknownFields: MessageBackupUnknownFields(fields: unknownFields)
        )
    case ZonaRosaErrorCodeRegistrationUnknown:
        throw RegistrationError.unknown(errStr)
    case ZonaRosaErrorCodeRegistrationInvalidSessionId:
        throw RegistrationError.invalidSessionId(errStr)
    case ZonaRosaErrorCodeRegistrationSessionNotFound:
        throw RegistrationError.sessionNotFound(errStr)
    case ZonaRosaErrorCodeRegistrationNotReadyForVerification:
        throw RegistrationError.notReadyForVerification(errStr)
    case ZonaRosaErrorCodeRegistrationSendVerificationCodeFailed:
        throw RegistrationError.sendVerificationFailed(errStr)
    case ZonaRosaErrorCodeRegistrationCodeNotDeliverable:
        let pair = try invokeFnReturningValueByPointer(.init()) {
            zonarosa_error_get_registration_error_not_deliverable($0, error)
        }
        defer { zonarosa_free_string(pair.first) }
        let message = String(cString: pair.first!)
        throw RegistrationError.codeNotDeliverable(message: message, permanentFailure: pair.second)
    case ZonaRosaErrorCodeRegistrationSessionUpdateRejected:
        throw RegistrationError.sessionUpdateRejected(errStr)
    case ZonaRosaErrorCodeRegistrationCredentialsCouldNotBeParsed:
        throw RegistrationError.credentialsCouldNotBeParsed(errStr)
    case ZonaRosaErrorCodeRegistrationDeviceTransferPossible:
        throw RegistrationError.deviceTransferPossible(errStr)
    case ZonaRosaErrorCodeRegistrationRecoveryVerificationFailed:
        throw RegistrationError.recoveryVerificationFailed(errStr)
    case ZonaRosaErrorCodeRegistrationLock:
        var timeRemaining: UInt64 = 0
        var svr2Password = ""
        let svr2Username = try invokeFnReturningString { svr2Username in
            var bridgedPassword: UnsafePointer<CChar>? = nil
            let err = zonarosa_error_get_registration_lock(&timeRemaining, svr2Username, &bridgedPassword, error)
            if err == nil {
                svr2Password = String(cString: bridgedPassword!)
                zonarosa_free_string(bridgedPassword)
            }
            return err
        }

        throw RegistrationError.registrationLock(
            timeRemaining: TimeInterval(timeRemaining),
            svr2Username: svr2Username,
            svr2Password: svr2Password
        )
    case ZonaRosaErrorCodeKeyTransparencyError:
        throw ZonaRosaError.keyTransparencyError(errStr)
    case ZonaRosaErrorCodeKeyTransparencyVerificationFailed:
        throw ZonaRosaError.keyTransparencyVerificationFailed(errStr)
    case ZonaRosaErrorCodeRequestUnauthorized:
        throw ZonaRosaError.requestUnauthorized(errStr)
    case ZonaRosaErrorCodeMismatchedDevices:
        var entries = ZonaRosaOwnedBufferOfFfiMismatchedDevicesError()
        try checkError(zonarosa_error_get_mismatched_device_errors(&entries, error))
        defer { zonarosa_free_list_of_mismatched_device_errors(entries) }
        throw ZonaRosaError.mismatchedDevices(
            entries: UnsafeBufferPointer(start: entries.base, count: entries.length).map { MismatchedDeviceEntry($0) },
            message: errStr
        )
    default:
        throw ZonaRosaError.unknown(errType, errStr)
    }
}

internal func failOnError(_ error: ZonaRosaFfiErrorRef?) {
    failOnError { try checkError(error) }
}

internal func failOnError<Result>(_ fn: () throws -> Result, file: StaticString = #file, line: UInt32 = #line) -> Result
{
    do {
        return try fn()
    } catch {
        guard let loggerBridge = LoggerBridge.shared else {
            fatalError("unexpected error: \(error)", file: file, line: UInt(line))
        }
        "unexpected error: \(error)".withCString {
            loggerBridge.logger.logFatal(file: String(describing: file), line: line, message: $0)
        }
    }
}
