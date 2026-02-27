//
// Copyright 2024 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
public import LibZonaRosaClient

public enum CallEnvelopeType {
    case offer(SSKProtoCallMessageOffer)
    case answer(SSKProtoCallMessageAnswer)
    case iceUpdate([SSKProtoCallMessageIceUpdate])
    case hangup(SSKProtoCallMessageHangup)
    case busy(SSKProtoCallMessageBusy)
    case opaque(SSKProtoCallMessageOpaque)
}

public protocol CallMessageHandler {
    func receivedEnvelope(
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
    )

    func receivedGroupCallUpdateMessage(
        _ updateMessage: SSKProtoDataMessageGroupCallUpdate,
        forGroupId groupId: GroupIdentifier,
        serverReceivedTimestamp: UInt64,
    ) async
}
