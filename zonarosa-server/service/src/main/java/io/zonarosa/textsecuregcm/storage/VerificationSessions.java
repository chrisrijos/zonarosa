/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.storage;

import java.time.Clock;
import io.zonarosa.server.registration.VerificationSession;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

public class VerificationSessions extends SerializedExpireableJsonDynamoStore<VerificationSession> {

  public VerificationSessions(final DynamoDbAsyncClient dynamoDbClient, final String tableName, final Clock clock) {
    super(dynamoDbClient, tableName, clock);
  }
}
