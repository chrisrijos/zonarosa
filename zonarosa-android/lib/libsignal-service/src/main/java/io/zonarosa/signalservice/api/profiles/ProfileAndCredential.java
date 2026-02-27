package io.zonarosa.service.api.profiles;

import io.zonarosa.libzonarosa.zkgroup.profiles.ExpiringProfileKeyCredential;

import java.util.Optional;


public final class ProfileAndCredential {

  private final ZonaRosaServiceProfile                   profile;
  private final ZonaRosaServiceProfile.RequestType       requestType;
  private final Optional<ExpiringProfileKeyCredential> expiringProfileKeyCredential;

  public ProfileAndCredential(ZonaRosaServiceProfile profile,
                              ZonaRosaServiceProfile.RequestType requestType,
                              Optional<ExpiringProfileKeyCredential> expiringProfileKeyCredential)
  {
    this.profile                      = profile;
    this.requestType                  = requestType;
    this.expiringProfileKeyCredential = expiringProfileKeyCredential;
  }

  public ZonaRosaServiceProfile getProfile() {
    return profile;
  }

  public ZonaRosaServiceProfile.RequestType getRequestType() {
    return requestType;
  }

  public Optional<ExpiringProfileKeyCredential> getExpiringProfileKeyCredential() {
    return expiringProfileKeyCredential;
  }
}
