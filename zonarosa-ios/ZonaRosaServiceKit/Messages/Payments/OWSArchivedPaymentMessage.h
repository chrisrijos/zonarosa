//
// Copyright 2024 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

#import <ZonaRosaServiceKit/TSPaymentModels.h>

#ifndef OWSRestoredPayment_h
#define OWSRestoredPayment_h

@protocol OWSArchivedPaymentMessage
@required
@property (nonatomic, readonly) TSArchivedPaymentInfo *archivedPaymentInfo;
@end

#endif /* OWSRestoredPayment_h */
