//
// Copyright 2019 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

#if TESTABLE_BUILD

import Foundation
import LibZonaRosaClient

extension InMemoryZonaRosaProtocolStore: WritablePreKeyStore {}
extension InMemoryZonaRosaProtocolStore: WritableSignedPreKeyStore {}
extension InMemoryZonaRosaProtocolStore: WritableKyberPreKeyStore {}

/// A helper for tests which can initializes ZonaRosa Protocol sessions
/// and then encrypt and decrypt messages for those sessions.
struct TestProtocolRunner {

    init() { }

    /// Sets up a session for `senderClient` to send to `recipientClient`, but not vice versa.
    ///
    /// Messages from `senderClient` will be PreKey messages.
    func initializePreKeys(
        senderClient: TestZonaRosaClient,
        recipientClient: TestZonaRosaClient,
        hasSignedPreKey: Bool = true,
        hasOneTimePreKey: Bool = true,
        transaction: DBWriteTransaction,
    ) throws {
        senderClient.ensureRecipientId(tx: transaction)
        recipientClient.ensureRecipientId(tx: transaction)

        let bobPreKey = PrivateKey.generate()
        let bobSignedPreKey = PrivateKey.generate()
        let bobKyberPreKey = KEMKeyPair.generate()

        let bobSignedPreKeyPublic = bobSignedPreKey.publicKey.serialize()
        let bobKyberPreKeyPublic = bobKyberPreKey.publicKey.serialize()

        let bobIdentityKey = recipientClient.identityKeyPair.identityKeyPair
        let bobSignedPreKeySignature = bobIdentityKey.privateKey.generateSignature(message: bobSignedPreKeyPublic)
        let bobKyberPreKeySignature = bobIdentityKey.privateKey.generateSignature(message: bobKyberPreKeyPublic)
        let bobRegistrationId = try recipientClient.identityKeyStore.localRegistrationId(context: transaction)

        let prekeyId: UInt32 = 4570
        let signedPrekeyId: UInt32 = 3006
        let kyberPrekeyId: UInt32 = 7777

        let bobBundle = try LibZonaRosaClient.PreKeyBundle(
            registrationId: bobRegistrationId,
            deviceId: recipientClient.deviceId,
            prekeyId: prekeyId,
            prekey: bobPreKey.publicKey,
            signedPrekeyId: signedPrekeyId,
            signedPrekey: bobSignedPreKey.publicKey,
            signedPrekeySignature: bobSignedPreKeySignature,
            identity: bobIdentityKey.identityKey,
            kyberPrekeyId: kyberPrekeyId,
            kyberPrekey: bobKyberPreKey.publicKey,
            kyberPrekeySignature: bobKyberPreKeySignature,
        )

        // Alice processes the bundle:
        try processPreKeyBundle(
            bobBundle,
            for: recipientClient.protocolAddress,
            sessionStore: senderClient.sessionStore,
            identityStore: senderClient.identityKeyStore,
            context: transaction,
        )

        // Bob does the same:
        if hasOneTimePreKey {
            try recipientClient.preKeyStore.storePreKey(
                LibZonaRosaClient.PreKeyRecord(id: prekeyId, privateKey: bobPreKey),
                replacedAt: nil,
                context: transaction,
            )
        }

        if hasSignedPreKey {
            try recipientClient.signedPreKeyStore.storeSignedPreKey(
                LibZonaRosaClient.SignedPreKeyRecord(
                    id: signedPrekeyId,
                    timestamp: 42000,
                    privateKey: bobSignedPreKey,
                    signature: bobSignedPreKeySignature,
                ),
                replacedAt: nil,
                context: transaction,
            )
        }

        try recipientClient.kyberPreKeyStore.storeKyberPreKey(
            LibZonaRosaClient.KyberPreKeyRecord(
                id: kyberPrekeyId,
                timestamp: 42000,
                keyPair: bobKyberPreKey,
                signature: bobKyberPreKeySignature,
            ),
            isOneTime: true,
            replacedAt: nil,
            context: transaction,
        )
    }

    /// Sets up a session between `senderClient` and `recipientClient`, so that either can talk to the other.
    ///
    /// Messages between both clients will be "Whisper" / "ciphertext" / "ZonaRosa" messages.
    func initialize(
        senderClient: TestZonaRosaClient,
        recipientClient: TestZonaRosaClient,
        hasSignedPreKey: Bool = true,
        hasOneTimePreKey: Bool = true,
        transaction: DBWriteTransaction,
    ) throws {
        try initializePreKeys(
            senderClient: senderClient,
            recipientClient: recipientClient,
            hasSignedPreKey: hasSignedPreKey,
            hasOneTimePreKey: hasOneTimePreKey,
            transaction: transaction,
        )

        // Then Alice sends a message to Bob so he gets her pre-key as well.
        let aliceMessage = try encrypt(
            Data(),
            senderClient: senderClient,
            recipient: recipientClient.protocolAddress,
            context: transaction,
        )
        _ = try zonarosaDecryptPreKey(
            message: PreKeyZonaRosaMessage(bytes: aliceMessage.serialize()),
            from: senderClient.protocolAddress,
            sessionStore: recipientClient.sessionStore,
            identityStore: recipientClient.identityKeyStore,
            preKeyStore: recipientClient.preKeyStore,
            signedPreKeyStore: recipientClient.signedPreKeyStore,
            kyberPreKeyStore: recipientClient.kyberPreKeyStore,
            context: transaction,
        )

        // Finally, Bob sends a message back to acknowledge the pre-key.
        let bobMessage = try encrypt(
            Data(),
            senderClient: recipientClient,
            recipient: senderClient.protocolAddress,
            context: transaction,
        )
        _ = try zonarosaDecrypt(
            message: ZonaRosaMessage(bytes: bobMessage.serialize()),
            from: recipientClient.protocolAddress,
            sessionStore: senderClient.sessionStore,
            identityStore: senderClient.identityKeyStore,
            context: transaction,
        )
    }

    func encrypt(
        _ plaintext: Data,
        senderClient: TestZonaRosaClient,
        recipient: ProtocolAddress,
        context: StoreContext,
    ) throws -> CiphertextMessage {
        return try zonarosaEncrypt(
            message: plaintext,
            for: recipient,
            sessionStore: senderClient.sessionStore,
            identityStore: senderClient.identityKeyStore,
            context: context,
        )
    }

    func decrypt(
        _ cipherMessage: CiphertextMessage,
        recipientClient: TestZonaRosaClient,
        sender: ProtocolAddress,
        context: StoreContext,
    ) throws -> Data {
        owsPrecondition(cipherMessage.messageType == .zonarosa, "only bare ZonaRosaMessages are supported")
        let message = try ZonaRosaMessage(bytes: cipherMessage.serialize())
        return try zonarosaDecrypt(
            message: message,
            from: sender,
            sessionStore: recipientClient.sessionStore,
            identityStore: recipientClient.identityKeyStore,
            context: context,
        )
    }
}

public typealias ZonaRosaE164Identifier = String
public typealias ZonaRosaAccountIdentifier = String

/// Represents a ZonaRosa installation, it can represent the local client or
/// a remote client.
protocol TestZonaRosaClient {
    var identityKeyPair: ECKeyPair { get }
    var identityKey: Data { get }
    var e164Identifier: ZonaRosaE164Identifier? { get }
    var serviceId: ServiceId { get }
    var deviceId: UInt32 { get }
    var address: ZonaRosaServiceAddress { get }
    var protocolAddress: ProtocolAddress { get }

    var sessionStore: LibZonaRosaClient.SessionStore { get }
    var preKeyStore: LibZonaRosaClient.PreKeyStore & WritablePreKeyStore { get }
    var signedPreKeyStore: LibZonaRosaClient.SignedPreKeyStore & WritableSignedPreKeyStore { get }
    var kyberPreKeyStore: LibZonaRosaClient.KyberPreKeyStore & WritableKyberPreKeyStore { get }
    var identityKeyStore: LibZonaRosaClient.IdentityKeyStore { get }
}

extension TestZonaRosaClient {
    var identityKey: Data {
        return identityKeyPair.publicKey
    }

    var address: ZonaRosaServiceAddress {
        return ZonaRosaServiceAddress(serviceId: serviceId, phoneNumber: e164Identifier)
    }

    var protocolAddress: ProtocolAddress {
        return ProtocolAddress(serviceId, deviceId: deviceId)
    }

    func ensureRecipientId(tx: DBWriteTransaction) {
        _ = DependenciesBridge.shared.recipientFetcher.fetchOrCreate(serviceId: serviceId, tx: tx)
    }
}

/// Can be used to represent the protocol state held by a remote client.
/// i.e. someone who's sending messages to the local client.
struct FakeZonaRosaClient: TestZonaRosaClient {

    var sessionStore: LibZonaRosaClient.SessionStore { return protocolStore }
    var preKeyStore: LibZonaRosaClient.PreKeyStore & WritablePreKeyStore { return protocolStore }
    var signedPreKeyStore: LibZonaRosaClient.SignedPreKeyStore & WritableSignedPreKeyStore { return protocolStore }
    var identityKeyStore: LibZonaRosaClient.IdentityKeyStore { return protocolStore }
    var kyberPreKeyStore: LibZonaRosaClient.KyberPreKeyStore & WritableKyberPreKeyStore { return protocolStore }

    let e164Identifier: ZonaRosaE164Identifier?
    let serviceId: ServiceId
    let protocolStore: InMemoryZonaRosaProtocolStore

    var deviceId = UInt32(1)
    var identityKeyPair: ECKeyPair {
        return ECKeyPair(try! protocolStore.identityKeyPair(context: NullContext()))
    }

    static func generate() -> FakeZonaRosaClient {
        return FakeZonaRosaClient(
            e164Identifier: CommonGenerator.e164(),
            serviceId: Aci.randomForTesting(),
            protocolStore: InMemoryZonaRosaProtocolStore(identity: .generate(), registrationId: 1),
        )
    }

    static func generate(
        e164Identifier: ZonaRosaE164Identifier? = nil,
        aci: Aci? = nil,
        deviceID: UInt32? = nil,
    ) -> FakeZonaRosaClient {
        var result = FakeZonaRosaClient(
            e164Identifier: e164Identifier,
            serviceId: aci ?? Aci.randomForTesting(),
            protocolStore: InMemoryZonaRosaProtocolStore(identity: .generate(), registrationId: 1),
        )
        if let deviceID {
            result.deviceId = deviceID
        }
        return result
    }
}

/// Represents the local user, backed by the same protocol stores, etc.
/// used in the app.
struct LocalZonaRosaClient: TestZonaRosaClient {
    let identity: OWSIdentity
    let _preKeyStore: PreKeyStore
    let _sessionStore: SessionManagerForIdentity

    init(identity: OWSIdentity = .aci) {
        self.identity = identity
        self._preKeyStore = PreKeyStore()
        self._sessionStore = SessionManagerForIdentity(
            identity: identity,
            recipientIdFinder: DependenciesBridge.shared.recipientIdFinder,
            sessionStore: SessionStore(),
        )
    }

    var identityKeyPair: ECKeyPair {
        return SSKEnvironment.shared.databaseStorageRef.read { tx in
            return DependenciesBridge.shared.identityManager.identityKeyPair(for: identity, tx: tx)!
        }
    }

    var e164Identifier: ZonaRosaE164Identifier? {
        return DependenciesBridge.shared.tsAccountManager.localIdentifiersWithMaybeSneakyTransaction?.phoneNumber
    }

    var serviceId: ServiceId {
        let localIdentifiers = DependenciesBridge.shared.tsAccountManager.localIdentifiersWithMaybeSneakyTransaction!
        switch identity {
        case .aci: return localIdentifiers.aci
        case .pni: return localIdentifiers.pni!
        }
    }

    let deviceId: UInt32 = 1

    var sessionStore: LibZonaRosaClient.SessionStore {
        return _sessionStore
    }

    var preKeyStore: LibZonaRosaClient.PreKeyStore & WritablePreKeyStore {
        return self._preKeyStore.forIdentity(self.identity)
    }

    var signedPreKeyStore: LibZonaRosaClient.SignedPreKeyStore & WritableSignedPreKeyStore {
        return self._preKeyStore.forIdentity(self.identity)
    }

    var kyberPreKeyStore: LibZonaRosaClient.KyberPreKeyStore & WritableKyberPreKeyStore {
        return self._preKeyStore.forIdentity(self.identity)
    }

    var identityKeyStore: IdentityKeyStore {
        return SSKEnvironment.shared.databaseStorageRef.read { transaction in
            return try! DependenciesBridge.shared.identityManager.libZonaRosaStore(for: identity, tx: transaction)
        }
    }

    func linkedDevice(deviceID: UInt32) -> FakeZonaRosaClient {
        return FakeZonaRosaClient(
            e164Identifier: e164Identifier,
            serviceId: serviceId,
            protocolStore: InMemoryZonaRosaProtocolStore(identity: identityKeyPair.identityKeyPair, registrationId: 1),
            deviceId: deviceID,
        )
    }
}

var envelopeId: UInt64 = 0

struct FakeService {
    let localClient: LocalZonaRosaClient
    let runner: TestProtocolRunner

    init(localClient: LocalZonaRosaClient, runner: TestProtocolRunner) {
        self.localClient = localClient
        self.runner = runner
    }

    func envelopeBuilder(fromSenderClient senderClient: TestZonaRosaClient, bodyText: String? = nil) throws -> SSKProtoEnvelopeBuilder {
        envelopeId += 1
        let builder = SSKProtoEnvelope.builder(timestamp: envelopeId)
        builder.setType(.ciphertext)
        builder.setSourceDevice(senderClient.deviceId)

        let timestamp = MessageTimestampGenerator.sharedInstance.generateTimestamp()
        builder.setTimestamp(timestamp)

        let content = try buildEncryptedContentData(fromSenderClient: senderClient, timestamp: timestamp, bodyText: bodyText)
        builder.setContent(content)

        // builder.setServerTimestamp(serverTimestamp)
        // builder.setServerGuid(serverGuid)

        return builder
    }

    func envelopeBuilder(fromSenderClient senderClient: TestZonaRosaClient, groupV2Context: SSKProtoGroupContextV2) throws -> SSKProtoEnvelopeBuilder {
        envelopeId += 1
        let builder = SSKProtoEnvelope.builder(timestamp: envelopeId)
        builder.setType(.ciphertext)
        builder.setSourceDevice(senderClient.deviceId)

        let content = try buildEncryptedContentData(fromSenderClient: senderClient, groupV2Context: groupV2Context)
        builder.setContent(content)

        // builder.setServerTimestamp(serverTimestamp)
        // builder.setServerGuid(serverGuid)

        return builder
    }

    func envelopeBuilderForServerGeneratedDeliveryReceipt(fromSenderClient senderClient: TestZonaRosaClient) -> SSKProtoEnvelopeBuilder {
        envelopeId += 1
        let builder = SSKProtoEnvelope.builder(timestamp: envelopeId)
        builder.setType(.receipt)
        builder.setSourceDevice(senderClient.deviceId)

        return builder
    }

    func envelopeBuilderForInvalidEnvelope(fromSenderClient senderClient: TestZonaRosaClient) -> SSKProtoEnvelopeBuilder {
        envelopeId += 1
        let builder = SSKProtoEnvelope.builder(timestamp: envelopeId)
        builder.setType(.unknown)
        builder.setSourceDevice(senderClient.deviceId)
        builder.setContent("Hello world".data(using: .utf8)!)

        return builder
    }

    func envelopeBuilderForUDDeliveryReceipt(fromSenderClient senderClient: TestZonaRosaClient, timestamp: UInt64) -> SSKProtoEnvelopeBuilder {
        envelopeId += 1
        let builder = SSKProtoEnvelope.builder(timestamp: envelopeId)
        builder.setType(.ciphertext)
        builder.setSourceDevice(senderClient.deviceId)

        let content = try! buildEncryptedContentData(fromSenderClient: senderClient, deliveryReceiptForMessage: timestamp)
        builder.setContent(content)

        return builder
    }

    func buildEncryptedContentData(fromSenderClient senderClient: TestZonaRosaClient, timestamp: UInt64, bodyText: String?) throws -> Data {
        let plaintext = try buildContentData(timestamp: timestamp, bodyText: bodyText)
        let cipherMessage: CiphertextMessage = SSKEnvironment.shared.databaseStorageRef.write { transaction in
            return try! self.runner.encrypt(
                plaintext,
                senderClient: senderClient,
                recipient: self.localClient.protocolAddress,
                context: transaction,
            )
        }

        assert(cipherMessage.messageType == .zonarosa)
        return cipherMessage.serialize()
    }

    func buildEncryptedContentData(fromSenderClient senderClient: TestZonaRosaClient, groupV2Context: SSKProtoGroupContextV2) throws -> Data {
        let plaintext = try buildContentData(groupV2Context: groupV2Context)
        let cipherMessage: CiphertextMessage = SSKEnvironment.shared.databaseStorageRef.write { transaction in
            return try! self.runner.encrypt(
                plaintext,
                senderClient: senderClient,
                recipient: self.localClient.protocolAddress,
                context: transaction,
            )
        }

        assert(cipherMessage.messageType == .zonarosa)
        return cipherMessage.serialize()
    }

    func buildEncryptedContentData(fromSenderClient senderClient: TestZonaRosaClient, deliveryReceiptForMessage timestamp: UInt64) throws -> Data {
        let plaintext = try buildContentData(deliveryReceiptForMessage: timestamp)
        let cipherMessage: CiphertextMessage = SSKEnvironment.shared.databaseStorageRef.write { transaction in
            return try! self.runner.encrypt(
                plaintext,
                senderClient: senderClient,
                recipient: self.localClient.protocolAddress,
                context: transaction,
            )
        }

        assert(cipherMessage.messageType == .zonarosa)
        return cipherMessage.serialize()
    }

    func buildContentData(timestamp: UInt64, bodyText: String?) throws -> Data {
        let dataMessageBuilder = SSKProtoDataMessage.builder()
        dataMessageBuilder.setTimestamp(timestamp)
        if let bodyText {
            dataMessageBuilder.setBody(bodyText)
        } else {
            dataMessageBuilder.setBody(CommonGenerator.paragraph)
        }

        let contentBuilder = SSKProtoContent.builder()
        contentBuilder.setDataMessage(try dataMessageBuilder.build())

        return try contentBuilder.buildSerializedData()
    }

    func buildSyncSentMessage(bodyText: String, recipient: ZonaRosaServiceAddress, timestamp: UInt64) throws -> Data {
        guard let destinationServiceId = recipient.serviceId else {
            owsFail("Cannot build sync message without a recipient UUID. Test is not set up correctly")
        }

        let dataMessageBuilder = SSKProtoDataMessage.builder()
        dataMessageBuilder.setBody(bodyText)
        dataMessageBuilder.setTimestamp(timestamp)

        let sentBuilder = SSKProtoSyncMessageSent.builder()
        sentBuilder.setMessage(try dataMessageBuilder.build())
        sentBuilder.setTimestamp(timestamp)
        sentBuilder.setDestinationServiceIDBinary(destinationServiceId.serviceIdBinary)
        let syncMessageBuilder = SSKProtoSyncMessage.builder()
        syncMessageBuilder.setSent(try sentBuilder.build())

        let contentBuilder = SSKProtoContent.builder()
        contentBuilder.setSyncMessage(try syncMessageBuilder.build())

        return try contentBuilder.buildSerializedData()
    }

    func buildContentData(groupV2Context: SSKProtoGroupContextV2) throws -> Data {
        let dataMessageBuilder = SSKProtoDataMessage.builder()
        dataMessageBuilder.setGroupV2(groupV2Context)

        let contentBuilder = SSKProtoContent.builder()
        contentBuilder.setDataMessage(try dataMessageBuilder.build())

        return try contentBuilder.buildSerializedData()
    }

    func buildContentData(deliveryReceiptForMessage timestamp: UInt64) throws -> Data {
        let receiptMessageBuilder = SSKProtoReceiptMessage.builder()
        receiptMessageBuilder.setType(.delivery)
        receiptMessageBuilder.setTimestamp([timestamp])

        let contentBuilder = SSKProtoContent.builder()
        contentBuilder.setReceiptMessage(receiptMessageBuilder.buildInfallibly())

        return try contentBuilder.buildSerializedData()
    }
}

#endif
