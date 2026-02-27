//
// Copyright 2024 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import LibZonaRosaClient

public struct GroupV2SnapshotResponse {
    let groupSnapshot: GroupV2Snapshot
    let groupSendEndorsementsResponse: GroupSendEndorsementsResponse?
}

public struct GroupV2Snapshot {
    let groupSecretParams: GroupSecretParams
    let revision: UInt32
    let title: String
    let descriptionText: String?
    let avatarUrlPath: String?
    let avatarDataState: TSGroupModel.AvatarDataState
    let groupMembership: GroupMembership
    let groupAccess: GroupAccess
    let inviteLinkPassword: Data?
    let disappearingMessageToken: DisappearingMessageToken
    let isAnnouncementsOnly: Bool
    let profileKeys: [Aci: Data]
}
