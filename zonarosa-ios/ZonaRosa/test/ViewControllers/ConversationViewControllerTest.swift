//
// Copyright 2020 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import XCTest

@testable import ZonaRosa

class ConversationViewControllerTest: ZonaRosaBaseTest {

    func testCVCBottomViewType() {
        XCTAssertEqual(CVCBottomViewType.none, CVCBottomViewType.none)
        XCTAssertNotEqual(CVCBottomViewType.none, CVCBottomViewType.inputToolbar)
        XCTAssertEqual(CVCBottomViewType.inputToolbar, CVCBottomViewType.inputToolbar)
        XCTAssertNotEqual(CVCBottomViewType.none, CVCBottomViewType.memberRequestView)
        XCTAssertNotEqual(
            CVCBottomViewType.memberRequestView,
            CVCBottomViewType.messageRequestView(
                messageRequestType: MessageRequestType(
                    isGroupV1Thread: true,
                    isGroupV2Thread: true,
                    isThreadBlocked: true,
                    hasSentMessages: true,
                    isThreadFromHiddenRecipient: false,
                    hasReportedSpam: false,
                    isLocalUserInvitedMember: false,
                ),
            ),
        )
        XCTAssertEqual(
            CVCBottomViewType.messageRequestView(
                messageRequestType: MessageRequestType(
                    isGroupV1Thread: true,
                    isGroupV2Thread: true,
                    isThreadBlocked: true,
                    hasSentMessages: true,
                    isThreadFromHiddenRecipient: false,
                    hasReportedSpam: false,
                    isLocalUserInvitedMember: false,
                ),
            ),
            CVCBottomViewType.messageRequestView(
                messageRequestType: MessageRequestType(
                    isGroupV1Thread: true,
                    isGroupV2Thread: true,
                    isThreadBlocked: true,
                    hasSentMessages: true,
                    isThreadFromHiddenRecipient: false,
                    hasReportedSpam: false,
                    isLocalUserInvitedMember: false,
                ),
            ),
        )
        XCTAssertNotEqual(
            CVCBottomViewType.messageRequestView(
                messageRequestType: MessageRequestType(
                    isGroupV1Thread: true,
                    isGroupV2Thread: true,
                    isThreadBlocked: true,
                    hasSentMessages: true,
                    isThreadFromHiddenRecipient: false,
                    hasReportedSpam: false,
                    isLocalUserInvitedMember: false,
                ),
            ),
            CVCBottomViewType.messageRequestView(
                messageRequestType: MessageRequestType(
                    isGroupV1Thread: true,
                    isGroupV2Thread: false,
                    isThreadBlocked: true,
                    hasSentMessages: true,
                    isThreadFromHiddenRecipient: false,
                    hasReportedSpam: false,
                    isLocalUserInvitedMember: false,
                ),
            ),
        )
        XCTAssertEqual(
            CVCBottomViewType.messageRequestView(
                messageRequestType: MessageRequestType(
                    isGroupV1Thread: true,
                    isGroupV2Thread: false,
                    isThreadBlocked: true,
                    hasSentMessages: true,
                    isThreadFromHiddenRecipient: false,
                    hasReportedSpam: false,
                    isLocalUserInvitedMember: false,
                ),
            ),
            CVCBottomViewType.messageRequestView(
                messageRequestType: MessageRequestType(
                    isGroupV1Thread: true,
                    isGroupV2Thread: false,
                    isThreadBlocked: true,
                    hasSentMessages: true,
                    isThreadFromHiddenRecipient: false,
                    hasReportedSpam: false,
                    isLocalUserInvitedMember: false,
                ),
            ),
        )
        XCTAssertNotEqual(
            CVCBottomViewType.messageRequestView(
                messageRequestType: MessageRequestType(
                    isGroupV1Thread: true,
                    isGroupV2Thread: true,
                    isThreadBlocked: true,
                    hasSentMessages: false,
                    isThreadFromHiddenRecipient: false,
                    hasReportedSpam: false,
                    isLocalUserInvitedMember: false,
                ),
            ),
            CVCBottomViewType.messageRequestView(
                messageRequestType: MessageRequestType(
                    isGroupV1Thread: true,
                    isGroupV2Thread: false,
                    isThreadBlocked: true,
                    hasSentMessages: true,
                    isThreadFromHiddenRecipient: false,
                    hasReportedSpam: false,
                    isLocalUserInvitedMember: false,
                ),
            ),
        )
    }
}
