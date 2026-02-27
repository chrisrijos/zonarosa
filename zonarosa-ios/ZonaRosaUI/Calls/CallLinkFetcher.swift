//
// Copyright 2024 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import LibZonaRosaClient
public import ZonaRosaRingRTC
public import ZonaRosaServiceKit

public struct CallLinkNotFoundError: Error {}

public class CallLinkFetcherImpl {
    private let sfuClient: SFUClient
    // Even though we never use this, we need to retain it to ensure
    // `sfuClient` continues to work properly.
    private let sfuClientHttpClient: AnyObject

    public init() {
        let httpClient = CallHTTPClient()
        self.sfuClient = ZonaRosaRingRTC.SFUClient(httpClient: httpClient.ringRtcHttpClient)
        self.sfuClientHttpClient = httpClient
    }

    public func readCallLink(
        _ rootKey: CallLinkRootKey,
        authCredential: ZonaRosaServiceKit.CallLinkAuthCredential,
    ) async throws -> ZonaRosaServiceKit.CallLinkState {
        let sfuUrl = DebugFlags.callingUseTestSFU.get() ? TSConstants.sfuTestURL : TSConstants.sfuURL
        let secretParams = CallLinkSecretParams.deriveFromRootKey(rootKey.bytes)
        let authCredentialPresentation = authCredential.present(callLinkParams: secretParams)
        do {
            return try await ZonaRosaServiceKit.CallLinkState(self.sfuClient.readCallLink(
                sfuUrl: sfuUrl,
                authCredentialPresentation: [UInt8](authCredentialPresentation.serialize()),
                linkRootKey: rootKey,
            ).unwrap())
        } catch where error.rawValue == 404 {
            throw CallLinkNotFoundError()
        }
    }
}

public struct SFUError: Error {
    let rawValue: UInt16
}

extension SFUResult {
    public func unwrap() throws(SFUError) -> Value {
        switch self {
        case .success(let value):
            return value
        case .failure(let errorCode):
            throw SFUError(rawValue: errorCode)
        }
    }
}
