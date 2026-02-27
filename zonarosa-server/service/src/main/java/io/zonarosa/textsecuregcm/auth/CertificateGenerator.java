/*
 * Copyright 2013-2020 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.auth;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.concurrent.TimeUnit;
import io.zonarosa.libzonarosa.protocol.ecc.ECPrivateKey;
import io.zonarosa.server.entities.MessageProtos.SenderCertificate;
import io.zonarosa.server.entities.MessageProtos.ServerCertificate;
import io.zonarosa.server.identity.IdentityType;
import io.zonarosa.server.storage.Account;
import io.zonarosa.server.util.UUIDUtil;

public class CertificateGenerator {

  private final ECPrivateKey privateKey;
  private final int expiresDays;
  private final boolean embedSigner;
  private final ServerCertificate serverCertificate;
  private final int serverCertificateId;

  public CertificateGenerator(byte[] serverCertificate, ECPrivateKey privateKey, int expiresDays, boolean embedSigner)
      throws InvalidProtocolBufferException {
    this.privateKey = privateKey;
    this.expiresDays = expiresDays;
    this.embedSigner = embedSigner;
    this.serverCertificate = ServerCertificate.parseFrom(serverCertificate);
    this.serverCertificateId = ServerCertificate.Certificate
        .parseFrom(this.serverCertificate.getCertificate())
        .getId();
  }

  public byte[] createFor(final Account account, final byte deviceId, boolean includeE164) {
    SenderCertificate.Certificate.Builder builder = SenderCertificate.Certificate.newBuilder()
        .setSenderDevice(Math.toIntExact(deviceId))
        .setExpires(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(expiresDays))
        .setIdentityKey(ByteString.copyFrom(account.getIdentityKey(IdentityType.ACI).serialize()))
        .setSenderUuid(UUIDUtil.toByteString(account.getUuid()));

    if (includeE164) {
      builder.setSenderE164(account.getNumber());
    }

    if (embedSigner) {
      builder.setSignerCertificate(serverCertificate);
    } else {
      builder.setSignerId(serverCertificateId);
    }

    byte[] certificate = builder.build().toByteArray();
    byte[] signature;
    signature = privateKey.calculateSignature(certificate);

    return SenderCertificate.newBuilder()
        .setCertificate(ByteString.copyFrom(certificate))
        .setSignature(ByteString.copyFrom(signature))
        .build()
        .toByteArray();
  }

}
