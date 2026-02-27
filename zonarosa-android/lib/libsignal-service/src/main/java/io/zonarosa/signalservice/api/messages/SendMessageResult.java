package io.zonarosa.service.api.messages;


import io.zonarosa.libzonarosa.protocol.IdentityKey;
import io.zonarosa.service.api.push.ZonaRosaServiceAddress;
import io.zonarosa.service.api.push.exceptions.ProofRequiredException;
import io.zonarosa.service.api.push.exceptions.RateLimitException;
import io.zonarosa.service.internal.push.Content;

import java.util.List;
import java.util.Optional;

public class SendMessageResult {

  private final ZonaRosaServiceAddress   address;
  private final Success                success;
  private final boolean                networkFailure;
  private final boolean                unregisteredFailure;
  private final IdentityFailure        identityFailure;
  private final ProofRequiredException proofRequiredFailure;
  private final RateLimitException     rateLimitFailure;
  private final boolean                invalidPreKeyFailure;
  private final boolean                canceledFailure;

  public static SendMessageResult success(ZonaRosaServiceAddress address, List<Integer> devices, boolean unidentified, boolean needsSync, long duration, Optional<Content> content) {
    return new SendMessageResult(address, new Success(unidentified, needsSync, duration, content, devices), false, false, null, null, null, false, false);
  }

  public static SendMessageResult networkFailure(ZonaRosaServiceAddress address) {
    return new SendMessageResult(address, null, true, false, null, null, null, false, false);
  }

  public static SendMessageResult unregisteredFailure(ZonaRosaServiceAddress address) {
    return new SendMessageResult(address, null, false, true, null, null, null, false, false);
  }

  public static SendMessageResult identityFailure(ZonaRosaServiceAddress address, IdentityKey identityKey) {
    return new SendMessageResult(address, null, false, false, new IdentityFailure(identityKey), null, null, false, false);
  }

  public static SendMessageResult proofRequiredFailure(ZonaRosaServiceAddress address, ProofRequiredException proofRequiredException) {
    return new SendMessageResult(address, null, false, false, null, proofRequiredException, null, false, false);
  }

  public static SendMessageResult rateLimitFailure(ZonaRosaServiceAddress address, RateLimitException rateLimitException) {
    return new SendMessageResult(address, null, false, false, null, null, rateLimitException, false, false);
  }

  public static SendMessageResult invalidPreKeyFailure(ZonaRosaServiceAddress address) {
    return new SendMessageResult(address, null, false, false, null, null, null, true, false);
  }

  public static SendMessageResult canceledFailure(ZonaRosaServiceAddress address) {
    return new SendMessageResult(address, null, false, false, null, null, null, false, true);
  }

  public ZonaRosaServiceAddress getAddress() {
    return address;
  }

  public Success getSuccess() {
    return success;
  }

  public boolean isSuccess() {
    return success != null;
  }

  public boolean isNetworkFailure() {
    return networkFailure || proofRequiredFailure != null || rateLimitFailure != null;
  }

  public boolean isUnregisteredFailure() {
    return unregisteredFailure;
  }

  public IdentityFailure getIdentityFailure() {
    return identityFailure;
  }

  public ProofRequiredException getProofRequiredFailure() {
    return proofRequiredFailure;
  }

  public RateLimitException getRateLimitFailure() {
    return rateLimitFailure;
  }

  public boolean isInvalidPreKeyFailure() {
    return invalidPreKeyFailure;
  }

  public boolean isCanceledFailure() {
    return canceledFailure;
  }

  private SendMessageResult(ZonaRosaServiceAddress address,
                            Success success,
                            boolean networkFailure,
                            boolean unregisteredFailure,
                            IdentityFailure identityFailure,
                            ProofRequiredException proofRequiredFailure,
                            RateLimitException rateLimitFailure,
                            boolean invalidPreKeyFailure,
                            boolean canceledFailure)
  {
    this.address              = address;
    this.success              = success;
    this.networkFailure       = networkFailure;
    this.unregisteredFailure  = unregisteredFailure;
    this.identityFailure      = identityFailure;
    this.proofRequiredFailure = proofRequiredFailure;
    this.rateLimitFailure     = rateLimitFailure;
    this.invalidPreKeyFailure = invalidPreKeyFailure;
    this.canceledFailure      = canceledFailure;
  }

  public static class Success {
    private final boolean           unidentified;
    private final boolean           needsSync;
    private final long              duration;
    private final Optional<Content> content;
    private final List<Integer>     devices;

    private Success(boolean unidentified, boolean needsSync, long duration, Optional<Content> content, List<Integer> devices) {
      this.unidentified = unidentified;
      this.needsSync    = needsSync;
      this.duration     = duration;
      this.content      = content;
      this.devices      = devices;
    }

    public boolean isUnidentified() {
      return unidentified;
    }

    public boolean isNeedsSync() {
      return needsSync;
    }

    public long getDuration() {
      return duration;
    }

    public Optional<Content> getContent() {
      return content;
    }

    public List<Integer> getDevices() {
      return devices;
    }
  }

  public static class IdentityFailure {
    private final IdentityKey identityKey;

    private IdentityFailure(IdentityKey identityKey) {
      this.identityKey = identityKey;
    }

    public IdentityKey getIdentityKey() {
      return identityKey;
    }
  }



}
