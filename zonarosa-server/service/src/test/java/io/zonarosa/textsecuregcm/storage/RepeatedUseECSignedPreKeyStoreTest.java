/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import io.zonarosa.libzonarosa.protocol.ecc.ECKeyPair;
import io.zonarosa.server.entities.ECSignedPreKey;
import io.zonarosa.server.tests.util.KeysHelper;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

class RepeatedUseECSignedPreKeyStoreTest extends RepeatedUseSignedPreKeyStoreTest<ECSignedPreKey> {

  private RepeatedUseECSignedPreKeyStore keyStore;

  private int currentKeyId = 1;

  @RegisterExtension
  static final DynamoDbExtension DYNAMO_DB_EXTENSION =
      new DynamoDbExtension(DynamoDbExtensionSchema.Tables.REPEATED_USE_EC_SIGNED_PRE_KEYS);

  private static final ECKeyPair IDENTITY_KEY_PAIR = ECKeyPair.generate();

  @BeforeEach
  void setUp() {
    keyStore = new RepeatedUseECSignedPreKeyStore(DYNAMO_DB_EXTENSION.getDynamoDbAsyncClient(),
        DynamoDbExtensionSchema.Tables.REPEATED_USE_EC_SIGNED_PRE_KEYS.tableName());
  }

  @Override
  protected RepeatedUseSignedPreKeyStore<ECSignedPreKey> getKeyStore() {
    return keyStore;
  }

  @Override
  protected ECSignedPreKey generateSignedPreKey() {
    return KeysHelper.signedECPreKey(currentKeyId++, IDENTITY_KEY_PAIR);
  }

  @Override
  protected DynamoDbClient getDynamoDbClient() {
    return DYNAMO_DB_EXTENSION.getDynamoDbClient();
  }
}
