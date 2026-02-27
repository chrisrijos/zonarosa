package io.zonarosa.service.api.messages;


import io.zonarosa.service.internal.push.BodyRange;

import java.util.List;
import java.util.Optional;

public class ZonaRosaServiceStoryMessage {
  private final Optional<byte[]>                      profileKey;
  private final Optional<ZonaRosaServiceGroupV2>        groupContext;
  private final Optional<ZonaRosaServiceAttachment>     fileAttachment;
  private final Optional<ZonaRosaServiceTextAttachment> textAttachment;
  private final Optional<Boolean>                     allowsReplies;
  private final Optional<List<BodyRange>>             bodyRanges;

  private ZonaRosaServiceStoryMessage(byte[] profileKey,
                                    ZonaRosaServiceGroupV2 groupContext,
                                    ZonaRosaServiceAttachment fileAttachment,
                                    ZonaRosaServiceTextAttachment textAttachment,
                                    boolean allowsReplies,
                                    List<BodyRange> bodyRanges)
  {
    this.profileKey     = Optional.ofNullable(profileKey);
    this.groupContext   = Optional.ofNullable(groupContext);
    this.fileAttachment = Optional.ofNullable(fileAttachment);
    this.textAttachment = Optional.ofNullable(textAttachment);
    this.allowsReplies  = Optional.of(allowsReplies);
    this.bodyRanges     = Optional.ofNullable(bodyRanges);
  }

  public static ZonaRosaServiceStoryMessage forFileAttachment(byte[] profileKey,
                                                            ZonaRosaServiceGroupV2 groupContext,
                                                            ZonaRosaServiceAttachment fileAttachment,
                                                            boolean allowsReplies,
                                                            List<BodyRange> bodyRanges)
  {
    return new ZonaRosaServiceStoryMessage(profileKey, groupContext, fileAttachment, null, allowsReplies, bodyRanges);
  }

  public static ZonaRosaServiceStoryMessage forTextAttachment(byte[] profileKey,
                                                            ZonaRosaServiceGroupV2 groupContext,
                                                            ZonaRosaServiceTextAttachment textAttachment,
                                                            boolean allowsReplies,
                                                            List<BodyRange> bodyRanges)
  {
    return new ZonaRosaServiceStoryMessage(profileKey, groupContext, null, textAttachment, allowsReplies, bodyRanges);
  }

  public Optional<byte[]> getProfileKey() {
    return profileKey;
  }

  public Optional<ZonaRosaServiceGroupV2> getGroupContext() {
    return groupContext;
  }

  public Optional<ZonaRosaServiceAttachment> getFileAttachment() {
    return fileAttachment;
  }

  public Optional<ZonaRosaServiceTextAttachment> getTextAttachment() {
    return textAttachment;
  }

  public Optional<Boolean> getAllowsReplies() {
    return allowsReplies;
  }

  public Optional<List<BodyRange>> getBodyRanges() {
    return bodyRanges;
  }
}
