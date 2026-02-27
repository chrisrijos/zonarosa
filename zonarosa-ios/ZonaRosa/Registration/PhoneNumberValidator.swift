//
// Copyright 2019 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import ZonaRosaServiceKit

struct PhoneNumberValidator {
    func isValidForRegistration(phoneNumber: E164) -> Bool {
        if phoneNumber.stringValue.hasPrefix("+1") {
            return isValidForUnitedStatesRegistration(phoneNumber: phoneNumber)
        }
        if phoneNumber.stringValue.hasPrefix("+55") {
            return isValidForBrazilRegistration(phoneNumber: phoneNumber)
        }
        // no extra validation for this country
        return true
    }

    private let validBrazilPhoneNumberRegex = try! NSRegularExpression(pattern: "^\\+55\\d{2}9?\\d{8}$", options: [])
    private func isValidForBrazilRegistration(phoneNumber: E164) -> Bool {
        validBrazilPhoneNumberRegex.hasMatch(input: phoneNumber.stringValue)
    }

    private let validUnitedStatesPhoneNumberRegex = try! NSRegularExpression(pattern: "^\\+1\\d{10}$", options: [])
    private func isValidForUnitedStatesRegistration(phoneNumber: E164) -> Bool {
        validUnitedStatesPhoneNumberRegex.hasMatch(input: phoneNumber.stringValue)
    }
}
