//
// Copyright 2022 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

#import <Foundation/Foundation.h>

//! Project version number for ZonaRosaServiceKit.
FOUNDATION_EXPORT double ZonaRosaServiceKitVersionNumber;

//! Project version string for ZonaRosaServiceKit.
FOUNDATION_EXPORT const unsigned char ZonaRosaServiceKitVersionString[];

#import <ZonaRosaServiceKit/BaseModel.h>
#import <ZonaRosaServiceKit/DebuggerUtils.h>
#import <ZonaRosaServiceKit/OWSAddToContactsOfferMessage.h>
#import <ZonaRosaServiceKit/OWSAddToProfileWhitelistOfferMessage.h>
#import <ZonaRosaServiceKit/OWSArchivedPaymentMessage.h>
#import <ZonaRosaServiceKit/OWSAsserts.h>
#import <ZonaRosaServiceKit/OWSDisappearingConfigurationUpdateInfoMessage.h>
#import <ZonaRosaServiceKit/OWSGroupCallMessage.h>
#import <ZonaRosaServiceKit/OWSIncomingArchivedPaymentMessage.h>
#import <ZonaRosaServiceKit/OWSIncomingPaymentMessage.h>
#import <ZonaRosaServiceKit/OWSLogs.h>
#import <ZonaRosaServiceKit/OWSOutgoingArchivedPaymentMessage.h>
#import <ZonaRosaServiceKit/OWSOutgoingPaymentMessage.h>
#import <ZonaRosaServiceKit/OWSPaymentMessage.h>
#import <ZonaRosaServiceKit/OWSReadTracking.h>
#import <ZonaRosaServiceKit/OWSRecoverableDecryptionPlaceholder.h>
#import <ZonaRosaServiceKit/OWSUnknownContactBlockOfferMessage.h>
#import <ZonaRosaServiceKit/OWSUnknownProtocolVersionMessage.h>
#import <ZonaRosaServiceKit/OWSVerificationState.h>
#import <ZonaRosaServiceKit/OWSVerificationStateChangeMessage.h>
#import <ZonaRosaServiceKit/SSKAccessors+SDS.h>
#import <ZonaRosaServiceKit/TSCall.h>
#import <ZonaRosaServiceKit/TSContactThread.h>
#import <ZonaRosaServiceKit/TSErrorMessage.h>
#import <ZonaRosaServiceKit/TSGroupModel.h>
#import <ZonaRosaServiceKit/TSGroupThread.h>
#import <ZonaRosaServiceKit/TSIncomingMessage.h>
#import <ZonaRosaServiceKit/TSInfoMessage.h>
#import <ZonaRosaServiceKit/TSInteraction.h>
#import <ZonaRosaServiceKit/TSInvalidIdentityKeyErrorMessage.h>
#import <ZonaRosaServiceKit/TSInvalidIdentityKeyReceivingErrorMessage.h>
#import <ZonaRosaServiceKit/TSInvalidIdentityKeySendingErrorMessage.h>
#import <ZonaRosaServiceKit/TSMessage.h>
#import <ZonaRosaServiceKit/TSOutgoingMessage.h>
#import <ZonaRosaServiceKit/TSPaymentModels.h>
#import <ZonaRosaServiceKit/TSPrivateStoryThread.h>
#import <ZonaRosaServiceKit/TSQuotedMessage.h>
#import <ZonaRosaServiceKit/TSThread.h>
#import <ZonaRosaServiceKit/TSUnreadIndicatorInteraction.h>
#import <ZonaRosaServiceKit/TSYapDatabaseObject.h>
#import <ZonaRosaServiceKit/Threading.h>

#define OWSLocalizedString(key, comment)                                                                               \
    [[NSBundle mainBundle].appBundle localizedStringForKey:(key) value:@"" table:nil]
