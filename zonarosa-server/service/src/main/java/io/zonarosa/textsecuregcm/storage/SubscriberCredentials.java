/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.server.storage;

import jakarta.ws.rs.InternalServerErrorException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import io.zonarosa.server.auth.AuthenticatedDevice;
import io.zonarosa.server.subscriptions.SubscriptionException;
import io.zonarosa.server.subscriptions.SubscriptionForbiddenException;
import io.zonarosa.server.subscriptions.SubscriptionNotFoundException;

public record SubscriberCredentials(@Nonnull byte[] subscriberBytes,
                             @Nonnull byte[] subscriberUser,
                             @Nonnull byte[] subscriberKey,
                             @Nonnull byte[] hmac,
                             @Nonnull Instant now) {

  public static SubscriberCredentials process(
      Optional<AuthenticatedDevice> authenticatedAccount,
      String subscriberId,
      Clock clock) throws SubscriptionException {
    Instant now = clock.instant();
    if (authenticatedAccount.isPresent()) {
      throw new SubscriptionForbiddenException("must not use authenticated connection for subscriber operations");
    }
    byte[] subscriberBytes = convertSubscriberIdStringToBytes(subscriberId);
    byte[] subscriberUser = getUser(subscriberBytes);
    byte[] subscriberKey = getKey(subscriberBytes);
    byte[] hmac = computeHmac(subscriberUser, subscriberKey);
    return new SubscriberCredentials(subscriberBytes, subscriberUser, subscriberKey, hmac, now);
  }

  private static byte[] convertSubscriberIdStringToBytes(String subscriberId) throws SubscriptionNotFoundException {
    try {
      byte[] bytes = Base64.getUrlDecoder().decode(subscriberId);
      if (bytes.length != 32) {
        throw new SubscriptionNotFoundException();
      }
      return bytes;
    } catch (IllegalArgumentException e) {
      throw new SubscriptionNotFoundException(e);
    }
  }

  private static byte[] getUser(byte[] subscriberBytes) {
    byte[] user = new byte[16];
    System.arraycopy(subscriberBytes, 0, user, 0, user.length);
    return user;
  }

  private static byte[] getKey(byte[] subscriberBytes) {
    byte[] key = new byte[16];
    System.arraycopy(subscriberBytes, 16, key, 0, key.length);
    return key;
  }

  private static byte[] computeHmac(byte[] subscriberUser, byte[] subscriberKey) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(subscriberKey, "HmacSHA256"));
      return mac.doFinal(subscriberUser);
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new InternalServerErrorException(e);
    }
  }
}
