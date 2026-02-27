package io.zonarosa.service.internal.push;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.zonarosa.libzonarosa.protocol.IdentityKey;
import io.zonarosa.core.models.ServiceId;
import io.zonarosa.service.internal.util.JsonUtil;
import io.zonarosa.core.util.Base64;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.annotation.Nonnull;

public class IdentityCheckRequest {
  @JsonProperty("elements")
  private final List<ServiceIdFingerprintPair> serviceIdFingerprintPairs;

  public IdentityCheckRequest(@Nonnull List<ServiceIdFingerprintPair> serviceIdKeyPairs) {
    this.serviceIdFingerprintPairs = serviceIdKeyPairs;
  }

  public List<ServiceIdFingerprintPair> getServiceIdFingerprintPairs() {
    return serviceIdFingerprintPairs;
  }

  public static final class ServiceIdFingerprintPair {

    @JsonProperty("uuid")
    @JsonSerialize(using = JsonUtil.ServiceIdSerializer.class)
    private final ServiceId serviceId;

    @JsonProperty
    private final String fingerprint;

    public ServiceIdFingerprintPair(@Nonnull ServiceId serviceId, @Nonnull IdentityKey identityKey) {
      this.serviceId = serviceId;

      try {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        this.fingerprint = Base64.encodeWithPadding(messageDigest.digest(identityKey.serialize()), 0, 4);
      } catch (NoSuchAlgorithmException e) {
        throw new AssertionError(e);
      }
    }

    public ServiceId getServiceId() {
      return serviceId;
    }

    public String getFingerprint() {
      return fingerprint;
    }
  }
}
