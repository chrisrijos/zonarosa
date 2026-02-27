//
// Copyright 2019 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import GRDB
import LibZonaRosaClient

public struct ZonaRosaAccountFinder {
    public init() {
    }

    public func zonarosaAccount(
        for e164: E164,
        tx: DBReadTransaction,
    ) -> ZonaRosaAccount? {
        return zonarosaAccount(for: e164.stringValue, tx: tx)
    }

    func zonarosaAccount(
        for phoneNumber: String,
        tx: DBReadTransaction,
    ) -> ZonaRosaAccount? {
        return zonarosaAccountWhere(
            column: ZonaRosaAccount.columnName(.recipientPhoneNumber),
            matches: phoneNumber,
            tx: tx,
        )
    }

    func zonarosaAccounts(
        for phoneNumbers: [String],
        tx: DBReadTransaction,
    ) -> [ZonaRosaAccount?] {
        return zonarosaAccountsForPhoneNumbers(phoneNumbers, tx: tx)
    }

    private func zonarosaAccountsForPhoneNumbers(
        _ phoneNumbers: [String?],
        tx: DBReadTransaction,
    ) -> [ZonaRosaAccount?] {
        let accounts = zonarosaAccountsWhere(
            column: ZonaRosaAccount.columnName(.recipientPhoneNumber),
            anyValueIn: phoneNumbers.compacted(),
            tx: tx,
        )

        let index: [String?: [ZonaRosaAccount?]] = Dictionary(grouping: accounts) { $0?.recipientPhoneNumber }
        return phoneNumbers.map { maybePhoneNumber -> ZonaRosaAccount? in
            guard
                let phoneNumber = maybePhoneNumber,
                let accountsForPhoneNumber = index[phoneNumber],
                let firstAccountForPhoneNumber = accountsForPhoneNumber.first
            else {
                return nil
            }

            return firstAccountForPhoneNumber
        }
    }

    private func zonarosaAccountsWhere(
        column: String,
        anyValueIn values: [String],
        tx: DBReadTransaction,
    ) -> [ZonaRosaAccount?] {
        guard !values.isEmpty else {
            return []
        }
        let qms = Array(repeating: "?", count: values.count).joined(separator: ", ")
        let sql = "SELECT * FROM \(ZonaRosaAccount.databaseTableName) WHERE \(column) in (\(qms))"

        return allZonaRosaAccounts(
            tx: tx,
            sql: sql,
            arguments: StatementArguments(values),
        )
    }

    private func zonarosaAccountWhere(
        column: String,
        matches matchString: String,
        tx: DBReadTransaction,
    ) -> ZonaRosaAccount? {
        let sql = "SELECT * FROM \(ZonaRosaAccount.databaseTableName) WHERE \(column) = ? LIMIT 1"

        return allZonaRosaAccounts(
            tx: tx,
            sql: sql,
            arguments: [matchString],
        ).first
    }

    private func allZonaRosaAccounts(
        tx: DBReadTransaction,
        sql: String,
        arguments: StatementArguments,
    ) -> [ZonaRosaAccount] {
        var result = [ZonaRosaAccount]()
        ZonaRosaAccount.anyEnumerate(
            transaction: tx,
            sql: sql,
            arguments: arguments,
        ) { account, _ in
            result.append(account)
        }
        return result
    }

    func fetchPhoneNumbers(tx: DBReadTransaction) throws -> [String] {
        let sql = """
            SELECT \(ZonaRosaAccount.columnName(.recipientPhoneNumber)) FROM \(ZonaRosaAccount.databaseTableName)
        """
        do {
            return try String?.fetchAll(tx.database, sql: sql).compacted()
        } catch {
            throw error.grdbErrorForLogging
        }
    }
}
