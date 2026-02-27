//
// Copyright 2023 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import LibZonaRosaClient

class SVR2WebsocketConfigurator: SgxWebsocketConfigurator {

    typealias Request = SVR2Proto_Request
    typealias Response = SVR2Proto_Response
    typealias Client = Svr2Client

    let mrenclave: MrEnclave
    var authMethod: SVR2.AuthMethod

    init(mrenclave: MrEnclave = TSConstants.svr2Enclave, authMethod: SVR2.AuthMethod) {
        self.mrenclave = mrenclave
        self.authMethod = authMethod
    }

    static var zonarosaServiceType: ZonaRosaServiceType { .svr2 }

    static func websocketUrlPath(mrenclaveString: String) -> String {
        return "v1/\(mrenclaveString)"
    }

    func fetchAuth() async throws -> RemoteAttestation.Auth {
        switch authMethod {
        case .svrAuth(let credential, _):
            return credential.credential
        case .chatServerAuth(let authedAccount):
            return try await RemoteAttestation.authForSVR2(chatServiceAuth: authedAccount.chatServiceAuth)
        case .implicit:
            return try await RemoteAttestation.authForSVR2(chatServiceAuth: .implicit())
        }
    }

    static func client(
        mrenclave: MrEnclave,
        attestationMessage: Data,
        currentDate: Date,
    ) throws -> Svr2Client {
        return try Svr2Client(
            mrenclave: mrenclave.dataValue,
            attestationMessage: attestationMessage,
            currentDate: currentDate,
        )
    }
}
