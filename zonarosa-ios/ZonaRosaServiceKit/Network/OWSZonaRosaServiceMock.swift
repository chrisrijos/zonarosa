//
// Copyright 2022 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation

#if TESTABLE_BUILD

public class OWSZonaRosaServiceMock: OWSZonaRosaServiceProtocol {
    public func warmCaches() {}

    public var isCensorshipCircumventionActive: Bool = false

    public var hasCensoredPhoneNumber: Bool = false

    public var isCensorshipCircumventionManuallyActivated: Bool = false

    public var isCensorshipCircumventionManuallyDisabled: Bool = false

    public var manualCensorshipCircumventionCountryCode: String?

    public func updateHasCensoredPhoneNumberDuringProvisioning(_ e164: E164) {}
    public func resetHasCensoredPhoneNumberFromProvisioning() {}

    public var urlEndpointBuilder: ((ZonaRosaServiceInfo) -> OWSURLSessionEndpoint)?

    public func buildUrlEndpoint(for zonarosaServiceInfo: ZonaRosaServiceInfo) -> OWSURLSessionEndpoint {
        return urlEndpointBuilder?(zonarosaServiceInfo) ?? OWSURLSessionEndpoint(
            baseUrl: zonarosaServiceInfo.baseUrl,
            frontingInfo: nil,
            securityPolicy: .systemDefault,
            extraHeaders: [:],
        )
    }

    public var mockUrlSessionBuilder: ((ZonaRosaServiceInfo, OWSURLSessionEndpoint, URLSessionConfiguration?) -> BaseOWSURLSessionMock)?

    public func buildUrlSession(
        for zonarosaServiceInfo: ZonaRosaServiceInfo,
        endpoint: OWSURLSessionEndpoint,
        configuration: URLSessionConfiguration?,
        maxResponseSize: UInt64?,
    ) -> OWSURLSessionProtocol {
        return mockUrlSessionBuilder?(zonarosaServiceInfo, endpoint, configuration) ?? BaseOWSURLSessionMock(
            endpoint: endpoint,
            configuration: .default,
            maxResponseSize: maxResponseSize,
        )
    }

    public var mockCDNUrlSessionBuilder: ((_ cdnNumber: UInt32) -> BaseOWSURLSessionMock)?

    public func sharedUrlSessionForCdn(
        cdnNumber: UInt32,
        maxResponseSize: UInt64?,
    ) async -> OWSURLSessionProtocol {
        let baseUrl: URL
        switch cdnNumber {
        case 0:
            baseUrl = URL(string: TSConstants.textSecureCDN0ServerURL)!
        case 3:
            baseUrl = URL(string: TSConstants.textSecureCDN3ServerURL)!
        default:
            baseUrl = URL(string: TSConstants.textSecureCDN2ServerURL)!
        }

        return mockCDNUrlSessionBuilder?(cdnNumber) ?? BaseOWSURLSessionMock(
            endpoint: OWSURLSessionEndpoint(
                baseUrl: baseUrl,
                frontingInfo: nil,
                securityPolicy: .systemDefault,
                extraHeaders: [:],
            ),
            configuration: .default,
            maxResponseSize: maxResponseSize,
        )
    }
}

#endif
