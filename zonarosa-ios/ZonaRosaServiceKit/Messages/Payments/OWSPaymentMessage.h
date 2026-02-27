//
// Copyright 2023 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

#import <ZonaRosaServiceKit/TSPaymentModels.h>

@protocol OWSPaymentMessage
@required

// Properties
@property (nonatomic, readonly, nullable) TSPaymentNotification *paymentNotification;

@end
