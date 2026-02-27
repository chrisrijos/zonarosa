//
// Copyright 2017 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

public import LibZonaRosaClient

public class NoopCallMessageHandler: CallMessageHandler {
    public init() {}

    public func receivedEnvelope(
        _ envelope: SSKProtoEnvelope,
        callEnvelope: CallEnvelopeType,
        from caller: (aci: Aci, deviceId: DeviceId),
        toLocalIdentity localIdentity: OWSIdentity,
        plaintextData: Data,
        wasReceivedByUD: Bool,
        sentAtTimestamp: UInt64,
        serverReceivedTimestamp: UInt64,
        serverDeliveryTimestamp: UInt64,
        tx: DBWriteTransaction,
    ) {
        owsFailDebug("")
    }

    public func receivedGroupCallUpdateMessage(
        _ updateMessage: SSKProtoDataMessageGroupCallUpdate,
        forGroupId groupId: GroupIdentifier,
        serverReceivedTimestamp: UInt64,
    ) async {
        owsFailDebug("")
    }
}
