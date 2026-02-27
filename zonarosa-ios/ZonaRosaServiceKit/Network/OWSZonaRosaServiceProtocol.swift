//
// Copyright 2022 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation

public protocol OWSZonaRosaServiceProtocol: AnyObject {
    func warmCaches()

    // MARK: - Censorship Circumvention

    var isCensorshipCircumventionActive: Bool { get }
    var hasCensoredPhoneNumber: Bool { get }
    var isCensorshipCircumventionManuallyActivated: Bool { get set }
    var isCensorshipCircumventionManuallyDisabled: Bool { get set }
    var manualCensorshipCircumventionCountryCode: String? { get set }

    func updateHasCensoredPhoneNumberDuringProvisioning(_ e164: E164)
    func resetHasCensoredPhoneNumberFromProvisioning()

    func buildUrlEndpoint(for zonarosaServiceInfo: ZonaRosaServiceInfo) -> OWSURLSessionEndpoint
    func buildUrlSession(
        for zonarosaServiceInfo: ZonaRosaServiceInfo,
        endpoint: OWSURLSessionEndpoint,
        configuration: URLSessionConfiguration?,
        maxResponseSize: UInt64?,
    ) -> OWSURLSessionProtocol

    func sharedUrlSessionForCdn(
        cdnNumber: UInt32,
        maxResponseSize: UInt64?,
    ) async -> OWSURLSessionProtocol
}

public enum ZonaRosaServiceType {
    case mainZonaRosaService
    case storageService
    case updates
    case updates2
    case svr2
}

// MARK: -

public extension OWSZonaRosaServiceProtocol {

    private func buildUrlSession(
        for zonarosaServiceType: ZonaRosaServiceType,
        configuration: URLSessionConfiguration? = nil,
        maxResponseSize: UInt64? = nil,
    ) -> OWSURLSessionProtocol {
        let zonarosaServiceInfo = zonarosaServiceType.zonarosaServiceInfo()
        return buildUrlSession(
            for: zonarosaServiceInfo,
            endpoint: buildUrlEndpoint(for: zonarosaServiceInfo),
            configuration: configuration,
            maxResponseSize: maxResponseSize,
        )
    }

    func urlSessionForMainZonaRosaService() -> OWSURLSessionProtocol {
        buildUrlSession(for: .mainZonaRosaService)
    }

    func urlSessionForStorageService() -> OWSURLSessionProtocol {
        buildUrlSession(for: .storageService)
    }

    func urlSessionForUpdates() -> OWSURLSessionProtocol {
        buildUrlSession(for: .updates)
    }

    func urlSessionForUpdates2() -> OWSURLSessionProtocol {
        buildUrlSession(for: .updates2)
    }
}

// MARK: - Service type mapping

public struct ZonaRosaServiceInfo {
    let baseUrl: URL
    let censorshipCircumventionSupported: Bool
    let censorshipCircumventionPathPrefix: String
    let shouldUseZonaRosaCertificate: Bool
    let shouldHandleRemoteDeprecation: Bool
    let type: ZonaRosaServiceType
}

extension ZonaRosaServiceType {

    public func zonarosaServiceInfo() -> ZonaRosaServiceInfo {
        switch self {
        case .mainZonaRosaService:
            return ZonaRosaServiceInfo(
                baseUrl: URL(string: TSConstants.mainServiceURL)!,
                censorshipCircumventionSupported: true,
                censorshipCircumventionPathPrefix: TSConstants.serviceCensorshipPrefix,
                shouldUseZonaRosaCertificate: true,
                shouldHandleRemoteDeprecation: true,
                type: self,
            )
        case .storageService:
            return ZonaRosaServiceInfo(
                baseUrl: URL(string: TSConstants.storageServiceURL)!,
                censorshipCircumventionSupported: true,
                censorshipCircumventionPathPrefix: TSConstants.storageServiceCensorshipPrefix,
                shouldUseZonaRosaCertificate: true,
                shouldHandleRemoteDeprecation: true,
                type: self,
            )
        case .updates:
            return ZonaRosaServiceInfo(
                baseUrl: URL(string: TSConstants.updatesURL)!,
                censorshipCircumventionSupported: false,
                censorshipCircumventionPathPrefix: "unimplemented",
                shouldUseZonaRosaCertificate: false,
                shouldHandleRemoteDeprecation: false,
                type: self,
            )
        case .updates2:
            return ZonaRosaServiceInfo(
                baseUrl: URL(string: TSConstants.updates2URL)!,
                censorshipCircumventionSupported: false,
                censorshipCircumventionPathPrefix: "unimplemented", // BADGES TODO
                shouldUseZonaRosaCertificate: true,
                shouldHandleRemoteDeprecation: false,
                type: self,
            )
        case .svr2:
            return ZonaRosaServiceInfo(
                baseUrl: URL(string: TSConstants.svr2URL)!,
                censorshipCircumventionSupported: true,
                censorshipCircumventionPathPrefix: TSConstants.svr2CensorshipPrefix,
                shouldUseZonaRosaCertificate: true,
                shouldHandleRemoteDeprecation: false,
                type: self,
            )
        }
    }
}
