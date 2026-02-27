/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.storage;

import java.util.Map;
import java.util.UUID;
import io.zonarosa.libzonarosa.protocol.InvalidKeyException;
import io.zonarosa.libzonarosa.protocol.ecc.ECPublicKey;
import io.zonarosa.server.entities.ECSignedPreKey;
import io.zonarosa.server.util.AttributeValues;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class RepeatedUseECSignedPreKeyStore extends RepeatedUseSignedPreKeyStore<ECSignedPreKey> {

  public RepeatedUseECSignedPreKeyStore(final DynamoDbAsyncClient dynamoDbAsyncClient, final String tableName) {
    super(dynamoDbAsyncClient, tableName);
  }

  @Override
  protected Map<String, AttributeValue> getItemFromPreKey(final UUID accountUuid, final byte deviceId, final ECSignedPreKey signedPreKey) {

    return Map.of(
        KEY_ACCOUNT_UUID, getPartitionKey(accountUuid),
        KEY_DEVICE_ID, getSortKey(deviceId),
        ATTR_KEY_ID, AttributeValues.fromLong(signedPreKey.keyId()),
        ATTR_PUBLIC_KEY, AttributeValues.fromByteArray(signedPreKey.serializedPublicKey()),
        ATTR_SIGNATURE, AttributeValues.fromByteArray(signedPreKey.signature()));
  }

  @Override
  protected ECSignedPreKey getPreKeyFromItem(final Map<String, AttributeValue> item) {
    try {
      return new ECSignedPreKey(
          Long.parseLong(item.get(ATTR_KEY_ID).n()),
          new ECPublicKey(item.get(ATTR_PUBLIC_KEY).b().asByteArray()),
          item.get(ATTR_SIGNATURE).b().asByteArray());
    } catch (final InvalidKeyException e) {
      // This should never happen since we're serializing keys directly from `ECPublicKey` instances on the way in
      throw new IllegalArgumentException(e);
    }
  }
}
