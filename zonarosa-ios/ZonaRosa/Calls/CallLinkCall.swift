//
// Copyright 2024 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import ZonaRosaRingRTC
import ZonaRosaServiceKit
import ZonaRosaUI

final class CallLinkCall: ZonaRosa.GroupCall {
    let callLink: CallLink
    let adminPasskey: Data?
    let callLinkState: ZonaRosaServiceKit.CallLinkState

    init(
        callLink: CallLink,
        adminPasskey: Data?,
        callLinkState: ZonaRosaServiceKit.CallLinkState,
        ringRtcCall: ZonaRosaRingRTC.GroupCall,
        videoCaptureController: VideoCaptureController,
    ) {
        self.callLink = callLink
        self.adminPasskey = adminPasskey
        self.callLinkState = callLinkState
        super.init(
            audioDescription: "[ZonaRosaCall] Call link call",
            ringRtcCall: ringRtcCall,
            videoCaptureController: videoCaptureController,
        )
    }

    var mayNeedToAskToJoin: Bool {
        return callLinkState.requiresAdminApproval && adminPasskey == nil
    }
}
