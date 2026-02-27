//
// Copyright 2024 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
public import ZonaRosaRingRTC

public class CallHTTPClient {
    public let ringRtcHttpClient: ZonaRosaRingRTC.HTTPClient

    public init() {
        self.ringRtcHttpClient = ZonaRosaRingRTC.HTTPClient()
        self.ringRtcHttpClient.delegate = self
    }
}

// MARK: - HTTPDelegate

extension CallHTTPClient: HTTPDelegate {
    /**
     * A HTTP request should be sent to the given url.
     * Invoked on the main thread, asychronously.
     * The result of the call should be indicated by calling the receivedHttpResponse() function.
     */
    public func sendRequest(requestId: UInt32, request: HTTPRequest) {
        AssertIsOnMainThread()

        let session = OWSURLSession(
            securityPolicy: OWSURLSession.zonarosaServiceSecurityPolicy,
            configuration: OWSURLSession.defaultConfigurationWithoutCaching,
            canUseZonaRosaProxy: true,
        )
        session.require2xxOr3xx = false
        session.allowRedirects = true
        session.customRedirectHandler = { redirectedRequest in
            var redirectedRequest = redirectedRequest
            if
                let authHeader = request.headers.first(where: {
                    $0.key.caseInsensitiveCompare("Authorization") == .orderedSame
                })
            {
                redirectedRequest.setValue(authHeader.value, forHTTPHeaderField: authHeader.key)
            }
            return redirectedRequest
        }

        Task { @MainActor in
            do {
                var headers = HttpHeaders()
                headers.addHeaderMap(request.headers, overwriteOnConflict: true)

                let response = try await session.performRequest(
                    request.url,
                    method: request.method.httpMethod,
                    headers: headers,
                    body: request.body,
                )
                self.ringRtcHttpClient.receivedResponse(
                    requestId: requestId,
                    response: response.asRingRTCResponse,
                )
            } catch {
                if error.isNetworkFailureOrTimeout {
                    Logger.warn("Peek client HTTP request had network error: \(error)")
                } else {
                    owsFailDebug("Peek client HTTP request failed \(error)")
                }
                self.ringRtcHttpClient.httpRequestFailed(requestId: requestId)
            }
        }
    }
}

extension ZonaRosaRingRTC.HTTPMethod {
    var httpMethod: ZonaRosaServiceKit.HTTPMethod {
        switch self {
        case .get: return .get
        case .post: return .post
        case .put: return .put
        case .delete: return .delete
        }
    }
}

extension ZonaRosaServiceKit.HTTPResponse {
    var asRingRTCResponse: ZonaRosaRingRTC.HTTPResponse {
        return ZonaRosaRingRTC.HTTPResponse(
            statusCode: UInt16(responseStatusCode),
            body: responseBodyData,
        )
    }
}
