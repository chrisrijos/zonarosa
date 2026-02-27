//
// Copyright 2024 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

@interface UIButton (DeprecationWorkaround)

@property (nonatomic, setter=ows_setAdjustsImageWhenDisabled:) BOOL ows_adjustsImageWhenDisabled;
@property (nonatomic, setter=ows_setAdjustsImageWhenHighlighted:) BOOL ows_adjustsImageWhenHighlighted;

@property (nonatomic, setter=ows_setContentEdgeInsets:) UIEdgeInsets ows_contentEdgeInsets;
@property (nonatomic, setter=ows_setImageEdgeInsets:) UIEdgeInsets ows_imageEdgeInsets;
@property (nonatomic, setter=ows_setTitleEdgeInsets:) UIEdgeInsets ows_titleEdgeInsets;

@end

NS_ASSUME_NONNULL_END
