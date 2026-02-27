//
// Copyright 2017 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

@import Foundation;

NS_ASSUME_NONNULL_BEGIN

@class GroupAccess;
@class GroupMembership;
@class ZonaRosaServiceAddress;

extern const NSUInteger kGroupIdLengthV1;
extern const NSUInteger kGroupIdLengthV2;
extern const uint64_t kMaxEncryptedAvatarSize;
extern const uint64_t kMaxAvatarSize;

typedef NS_CLOSED_ENUM(uint32_t, GroupsVersion) {
    GroupsVersionV1 = 0,
    GroupsVersionV2
};

// NOTE: This class is tightly coupled to TSGroupModelBuilder.
//       If you modify this class - especially if you
//       add any new properties - make sure to update
//       TSGroupModelBuilder.
@interface TSGroupModel : NSObject <NSSecureCoding, NSCopying>

// groupMembers includes administrators and normal members.
@property (nonatomic, readonly) NSArray<ZonaRosaServiceAddress *> *groupMembers;
@property (nonatomic, readonly, nullable) NSString *groupName;
@property (nonatomic, readonly) NSData *groupId;
@property (nonatomic, readonly, nullable) ZonaRosaServiceAddress *addedByAddress;

#if TARGET_OS_IOS
// This data should always be in PNG format.
@property (nonatomic, nullable) NSData *legacyAvatarData;
@property (nonatomic, nullable) NSString *avatarHash;

@property (nonatomic, readonly) GroupsVersion groupsVersion;
@property (nonatomic, readonly) GroupMembership *groupMembership;

+ (instancetype)new NS_UNAVAILABLE;
- (instancetype)init NS_UNAVAILABLE;
- (nullable instancetype)initWithCoder:(NSCoder *)coder NS_DESIGNATED_INITIALIZER;

- (instancetype)initWithGroupId:(NSData *)groupId
                           name:(nullable NSString *)name
                     avatarData:(nullable NSData *)avatarData
                        members:(NSArray<ZonaRosaServiceAddress *> *)members
                 addedByAddress:(nullable ZonaRosaServiceAddress *)addedByAddress NS_DESIGNATED_INITIALIZER;
#endif

@property (nonatomic, readonly) NSString *groupNameOrDefault;

@end

NS_ASSUME_NONNULL_END
