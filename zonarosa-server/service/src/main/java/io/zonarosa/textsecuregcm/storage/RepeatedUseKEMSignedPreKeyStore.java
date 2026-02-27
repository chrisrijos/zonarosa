/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.storage;

import io.zonarosa.libzonarosa.protocol.InvalidKeyException;
import io.zonarosa.libzonarosa.protocol.kem.KEMPublicKey;
import io.zonarosa.server.entities.KEMSignedPreKey;
import io.zonarosa.server.util.AttributeValues;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import java.util.Map;
import java.util.UUID;

public class RepeatedUseKEMSignedPreKeyStore extends RepeatedUseSignedPreKeyStore<KEMSignedPreKey> {

  public RepeatedUseKEMSignedPreKeyStore(final DynamoDbAsyncClient dynamoDbAsyncClient, final String tableName) {
    super(dynamoDbAsyncClient, tableName);
  }

  @Override
  protected Map<String, AttributeValue> getItemFromPreKey(final UUID accountUuid, final byte deviceId, final KEMSignedPreKey signedPreKey) {

    return Map.of(
        KEY_ACCOUNT_UUID, getPartitionKey(accountUuid),
        KEY_DEVICE_ID, getSortKey(deviceId),
        ATTR_KEY_ID, AttributeValues.fromLong(signedPreKey.keyId()),
        ATTR_PUBLIC_KEY, AttributeValues.fromByteArray(signedPreKey.serializedPublicKey()),
        ATTR_SIGNATURE, AttributeValues.fromByteArray(signedPreKey.signature()));
  }

  @Override
  protected KEMSignedPreKey getPreKeyFromItem(final Map<String, AttributeValue> item) {
    try {
      return new KEMSignedPreKey(
          Long.parseLong(item.get(ATTR_KEY_ID).n()),
          new KEMPublicKey(item.get(ATTR_PUBLIC_KEY).b().asByteArray()),
          item.get(ATTR_SIGNATURE).b().asByteArray());
    } catch (final InvalidKeyException e) {
      // This should never happen since we're serializing keys directly from `KEMPublicKey` instances on the way in
      throw new IllegalArgumentException(e);
    }
  }
}
