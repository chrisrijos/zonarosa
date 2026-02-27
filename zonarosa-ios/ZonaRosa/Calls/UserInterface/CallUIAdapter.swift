//
// Copyright 2016 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

public import Foundation
import CallKit
import ZonaRosaServiceKit
import ZonaRosaUI
import UIKit
import WebRTC

protocol CallUIAdaptee: AnyObject {
    var callService: CallService { get }

    init(showNamesOnCallScreen: Bool, useSystemCallLog: Bool)

    @MainActor
    func startOutgoingCall(call: ZonaRosaCall)

    // TODO: It might be nice to prevent call links from being passed here at compile time.
    @MainActor
    func reportIncomingCall(_ call: ZonaRosaCall, completion: @escaping (Error?) -> Void)

    @MainActor
    func answerCall(_ call: ZonaRosaCall)

    @MainActor
    func recipientAcceptedCall(_ call: CallMode)

    @MainActor
    func localHangupCall(_ call: ZonaRosaCall)

    @MainActor
    func remoteDidHangupCall(_ call: ZonaRosaCall)

    @MainActor
    func remoteBusy(_ call: ZonaRosaCall)

    @MainActor
    func didAnswerElsewhere(call: ZonaRosaCall)

    @MainActor
    func didDeclineElsewhere(call: ZonaRosaCall)

    @MainActor
    func wasBusyElsewhere(call: ZonaRosaCall)

    @MainActor
    func failCall(_ call: ZonaRosaCall, error: CallError)

    @MainActor
    func setIsMuted(call: ZonaRosaCall, isMuted: Bool)

    @MainActor
    func setHasLocalVideo(call: ZonaRosaCall, hasLocalVideo: Bool)
}

/**
 * Notify the user of call related activities.
 * Driven by either a CallKit or System notifications adaptee
 */
public class CallUIAdapter: NSObject {

    private var callService: CallService { AppEnvironment.shared.callService }

    private lazy var adaptee: any CallUIAdaptee = { () -> any CallUIAdaptee in
        let callUIAdapteeType: CallUIAdaptee.Type
#if targetEnvironment(simulator)
        callUIAdapteeType = SimulatorCallUIAdaptee.self
#else
        callUIAdapteeType = CallKitCallUIAdaptee.self
#endif
        let (showNames, useSystemCallLog) = SSKEnvironment.shared.databaseStorageRef.read { tx in
            return (
                SSKEnvironment.shared.preferencesRef.notificationPreviewType(tx: tx) != .noNameNoPreview,
                SSKEnvironment.shared.preferencesRef.isSystemCallLogEnabled(tx: tx),
            )
        }
        return callUIAdapteeType.init(
            showNamesOnCallScreen: showNames,
            useSystemCallLog: useSystemCallLog,
        )
    }()

    @MainActor
    override public init() {
        super.init()

        // We cannot assert singleton here, because this class gets rebuilt when the user changes relevant call settings
    }

    @MainActor
    func reportIncomingCall(_ call: ZonaRosaCall) {
        guard let caller = call.caller else {
            return
        }
        Logger.info("remoteAddress: \(caller)")

        // make sure we don't terminate audio session during call
        _ = SUIEnvironment.shared.audioSessionRef.startAudioActivity(call.commonState.audioActivity)

        adaptee.reportIncomingCall(call) { error in
            AssertIsOnMainThread()

            guard var error else {
                self.showCall(call)
                return
            }

            Logger.warn("error: \(error)")

            switch error {
            case CXErrorCodeIncomingCallError.filteredByDoNotDisturb:
                error = CallError.doNotDisturbEnabled
            case CXErrorCodeIncomingCallError.filteredByBlockList:
                error = CallError.contactIsBlocked
            default:
                break
            }

            self.callService.handleFailedCall(failedCall: call, error: error)
        }
    }

    @MainActor
    func reportMissedCall(_ call: ZonaRosaCall, individualCall: IndividualCall) {
        guard let callerAci = individualCall.thread.contactAddress.aci else {
            owsFailDebug("Can't receive a call without an ACI.")
            return
        }

        let sentAtTimestamp = Date(millisecondsSince1970: individualCall.sentAtTimestamp)
        SSKEnvironment.shared.databaseStorageRef.read { tx in
            SSKEnvironment.shared.notificationPresenterRef.notifyUserOfMissedCall(
                notificationInfo: CallNotificationInfo(
                    groupingId: individualCall.commonState.localId,
                    thread: individualCall.thread,
                    caller: callerAci,
                ),
                offerMediaType: individualCall.offerMediaType,
                sentAt: sentAtTimestamp,
                tx: tx,
            )
        }
    }

    @MainActor
    func startOutgoingCall(call: ZonaRosaCall) {
        // make sure we don't terminate audio session during call
        _ = SUIEnvironment.shared.audioSessionRef.startAudioActivity(call.commonState.audioActivity)

        adaptee.startOutgoingCall(call: call)
    }

    @MainActor
    func answerCall(_ call: ZonaRosaCall) {
        adaptee.answerCall(call)
    }

    @MainActor
    func startAndShowOutgoingCall(thread: TSContactThread, prepareResult: CallStarter.PrepareToStartCallResult, hasLocalVideo: Bool) {
        guard
            let (call, individualCall) = self.callService.buildOutgoingIndividualCallIfPossible(
                thread: thread,
                localDeviceId: prepareResult.localDeviceId,
                hasVideo: hasLocalVideo,
            )
        else {
            // @integration This is not unexpected, it could happen if Bob tries
            // to start an outgoing call at the same moment Alice has already
            // sent him an Offer that is being processed.
            Logger.info("found an existing call when trying to start outgoing call: \(thread.contactAddress)")
            return
        }

        startOutgoingCall(call: call)
        individualCall.hasLocalVideo = hasLocalVideo
        self.showCall(call)
    }

    @MainActor
    func recipientAcceptedCall(_ call: CallMode) {
        adaptee.recipientAcceptedCall(call)
    }

    @MainActor
    func remoteDidHangupCall(_ call: ZonaRosaCall) {
        adaptee.remoteDidHangupCall(call)
    }

    @MainActor
    func remoteBusy(_ call: ZonaRosaCall) {
        adaptee.remoteBusy(call)
    }

    @MainActor
    func didAnswerElsewhere(call: ZonaRosaCall) {
        adaptee.didAnswerElsewhere(call: call)
    }

    @MainActor
    func didDeclineElsewhere(call: ZonaRosaCall) {
        adaptee.didDeclineElsewhere(call: call)
    }

    @MainActor
    func wasBusyElsewhere(call: ZonaRosaCall) {
        adaptee.wasBusyElsewhere(call: call)
    }

    @MainActor
    func localHangupCall(_ call: ZonaRosaCall) {
        adaptee.localHangupCall(call)
    }

    @MainActor
    func failCall(_ call: ZonaRosaCall, error: CallError) {
        adaptee.failCall(call, error: error)
    }

    @MainActor
    private func showCall(_ call: ZonaRosaCall) {
        guard !call.hasTerminated else {
            Logger.info("Not showing window for terminated call \(call)")
            return
        }

        Logger.info("\(call)")

        let callViewController: UIViewController & CallViewControllerWindowReference
        switch call.mode {
        case .individual(let individualCall):
            callViewController = IndividualCallViewController(call: call, individualCall: individualCall)
        case .groupThread(let groupCall as GroupCall), .callLink(let groupCall as GroupCall):
            callViewController = SSKEnvironment.shared.databaseStorageRef.read { tx in
                return GroupCallViewController.load(call: call, groupCall: groupCall, tx: tx)
            }
        }

        callViewController.modalTransitionStyle = .crossDissolve
        AppEnvironment.shared.windowManagerRef.startCall(viewController: callViewController)
    }

    @MainActor
    func setIsMuted(call: ZonaRosaCall, isMuted: Bool) {
        // With CallKit, muting is handled by a CXAction, so it must go through the adaptee
        adaptee.setIsMuted(call: call, isMuted: isMuted)
    }

    @MainActor
    func setHasLocalVideo(call: ZonaRosaCall, hasLocalVideo: Bool) {
        adaptee.setHasLocalVideo(call: call, hasLocalVideo: hasLocalVideo)
    }

    @MainActor
    func setCameraSource(call: ZonaRosaCall, isUsingFrontCamera: Bool) {
        callService.updateCameraSource(call: call, isUsingFrontCamera: isUsingFrontCamera)
    }
}
