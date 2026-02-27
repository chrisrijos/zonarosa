package io.zonarosa.service.internal.push;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.zonarosa.libzonarosa.protocol.IdentityKey;
import io.zonarosa.service.api.push.SignedPreKeyEntity;
import io.zonarosa.service.internal.util.JsonUtil;

import java.util.List;

public class PreKeyState {

  @JsonProperty("preKeys")
  private List<PreKeyEntity> oneTimeEcPreKeys;

  @JsonProperty("signedPreKey")
  private SignedPreKeyEntity signedPreKey;

  @JsonProperty("pqLastResortPreKey")
  private KyberPreKeyEntity lastResortKyberKey;

  @JsonProperty("pqPreKeys")
  private List<KyberPreKeyEntity> oneTimeKyberKeys;

  public PreKeyState() {}

  public PreKeyState(
      SignedPreKeyEntity signedPreKey,
      List<PreKeyEntity> oneTimeEcPreKeys,
      KyberPreKeyEntity lastResortKyberPreKey,
      List<KyberPreKeyEntity> oneTimeKyberPreKeys
  ) {
    this.signedPreKey       = signedPreKey;
    this.oneTimeEcPreKeys   = oneTimeEcPreKeys;
    this.lastResortKyberKey = lastResortKyberPreKey;
    this.oneTimeKyberKeys   = oneTimeKyberPreKeys;
  }

  public List<PreKeyEntity> getPreKeys() {
    return oneTimeEcPreKeys;
  }

  public SignedPreKeyEntity getSignedPreKey() {
    return signedPreKey;
  }
}
