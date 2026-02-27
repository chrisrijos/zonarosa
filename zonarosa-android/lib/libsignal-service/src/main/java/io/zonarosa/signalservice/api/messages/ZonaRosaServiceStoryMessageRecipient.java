package io.zonarosa.service.api.messages;

import io.zonarosa.service.api.push.ZonaRosaServiceAddress;

import java.util.List;

public class ZonaRosaServiceStoryMessageRecipient {

  private final ZonaRosaServiceAddress zonarosaServiceAddress;
  private final List<String>         distributionListIds;
  private final boolean              isAllowedToReply;

  public ZonaRosaServiceStoryMessageRecipient(ZonaRosaServiceAddress zonarosaServiceAddress,
                                            List<String> distributionListIds,
                                            boolean isAllowedToReply)
  {
    this.zonarosaServiceAddress = zonarosaServiceAddress;
    this.distributionListIds  = distributionListIds;
    this.isAllowedToReply     = isAllowedToReply;
  }

  public List<String> getDistributionListIds() {
    return distributionListIds;
  }

  public ZonaRosaServiceAddress getZonaRosaServiceAddress() {
    return zonarosaServiceAddress;
  }

  public boolean isAllowedToReply() {
    return isAllowedToReply;
  }
}
