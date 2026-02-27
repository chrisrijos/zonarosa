//
// Copyright 2023 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import LibZonaRosaClient

extension Usernames {
    public class HashedUsername {
        private typealias LibZonaRosaUsername = LibZonaRosaClient.Username

        // MARK: Init

        private let libZonaRosaUsername: LibZonaRosaUsername

        public convenience init(forUsername username: String) throws {
            self.init(libZonaRosaUsername: try .init(username))
        }

        private init(libZonaRosaUsername: LibZonaRosaUsername) {
            self.libZonaRosaUsername = libZonaRosaUsername
        }

        // MARK: Getters

        /// The raw username.
        public var usernameString: String {
            libZonaRosaUsername.value
        }

        /// The hash of this username, as bytes.
        var rawHash: Data {
            libZonaRosaUsername.hash
        }

        /// The hash of this username, base64url-encoded.
        lazy var hashString: String = {
            libZonaRosaUsername.hash.asBase64Url
        }()

        /// The ZKProof string for this username's hash.
        lazy var proofString: String = {
            libZonaRosaUsername.generateProof().asBase64Url
        }()
    }
}

// MARK: - Generate candidates

public extension Usernames.HashedUsername {
    struct GeneratedCandidates {
        private let candidates: [Usernames.HashedUsername]

        fileprivate init(candidates: [Usernames.HashedUsername]) {
            self.candidates = candidates
        }

        var candidateHashes: [String] {
            candidates.map { $0.hashString }
        }

        func candidate(matchingHash hashString: String) -> Usernames.HashedUsername? {
            candidates.first(where: { candidate in
                candidate.hashString == hashString
            })
        }
    }

    enum CandidateGenerationError: Error {
        case nicknameCannotBeEmpty
        case nicknameCannotStartWithDigit
        case nicknameContainsInvalidCharacters
        case nicknameTooShort
        case nicknameTooLong

        fileprivate init?(fromZonaRosaError zonarosaError: LibZonaRosaClient.ZonaRosaError?) {
            guard let zonarosaError else { return nil }

            switch zonarosaError {
            case .nicknameCannotBeEmpty:
                self = .nicknameCannotBeEmpty
            case .nicknameCannotStartWithDigit:
                self = .nicknameCannotStartWithDigit
            case .badNicknameCharacter:
                self = .nicknameContainsInvalidCharacters
            case .nicknameTooShort:
                self = .nicknameTooShort
            case .nicknameTooLong:
                self = .nicknameTooLong
            default:
                return nil
            }
        }
    }

    static func generateCandidates(
        forNickname nickname: String,
        minNicknameLength: UInt32,
        maxNicknameLength: UInt32,
        desiredDiscriminator: String?,
    ) throws -> GeneratedCandidates {
        do {
            let nicknameLengthRange = minNicknameLength...maxNicknameLength
            if let desiredDiscriminator {
                let username = try LibZonaRosaUsername(nickname: nickname, discriminator: desiredDiscriminator, withValidLengthWithin: nicknameLengthRange)
                return .init(candidates: [.init(libZonaRosaUsername: username)])
            }

            let candidates: [Usernames.HashedUsername] = try LibZonaRosaUsername.candidates(
                from: nickname,
                withValidLengthWithin: nicknameLengthRange,
            ).map { candidate -> Usernames.HashedUsername in
                return .init(libZonaRosaUsername: candidate)
            }

            return GeneratedCandidates(candidates: candidates)
        } catch let error {
            if
                let libZonaRosaError = error as? ZonaRosaError,
                let generationError = CandidateGenerationError(fromZonaRosaError: libZonaRosaError)
            {
                throw generationError
            }

            throw error
        }
    }
}

// MARK: - Equatable

extension Usernames.HashedUsername: Equatable {
    public static func ==(lhs: Usernames.HashedUsername, rhs: Usernames.HashedUsername) -> Bool {
        lhs.libZonaRosaUsername.value == rhs.libZonaRosaUsername.value
    }
}
