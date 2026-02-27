//
// Copyright 2024 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
public import GRDB

public struct NicknameRecord: Codable, FetchableRecord, PersistableRecord, Equatable {
    public static let databaseTableName: String = "NicknameRecord"

    enum CodingKeys: String, CodingKey, ColumnExpression {
        case recipientRowID
        case givenName
        case familyName
        case note
    }

    public let recipientRowID: ZonaRosaRecipient.RowId
    public let givenName: String?
    public let familyName: String?
    public let note: String?

    public init(recipient: ZonaRosaRecipient, givenName: String?, familyName: String?, note: String?) {
        self.init(recipientRowID: recipient.id, givenName: givenName, familyName: familyName, note: note)
    }

    public init(recipientRowID: ZonaRosaRecipient.RowId, givenName: String?, familyName: String?, note: String?) {
        self.recipientRowID = recipientRowID
        self.givenName = givenName
        self.familyName = familyName
        self.note = note
    }
}
