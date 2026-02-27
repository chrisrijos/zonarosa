/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.storage;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import io.zonarosa.server.storage.DynamoDbExtensionSchema.Tables;
import io.zonarosa.server.util.UUIDUtil;

class ReportMessageDynamoDbTest {

  private ReportMessageDynamoDb reportMessageDynamoDb;

  @RegisterExtension
  static DynamoDbExtension DYNAMO_DB_EXTENSION = new DynamoDbExtension(Tables.REPORT_MESSAGES);


  @BeforeEach
  void setUp() {
    this.reportMessageDynamoDb = new ReportMessageDynamoDb(
        DYNAMO_DB_EXTENSION.getDynamoDbClient(),
        DYNAMO_DB_EXTENSION.getDynamoDbAsyncClient(),
        Tables.REPORT_MESSAGES.tableName(),
        Duration.ofDays(1));
  }

  @Test
  void testStore() {

    final byte[] hash1 = UUIDUtil.toBytes(UUID.randomUUID());
    final byte[] hash2 = UUIDUtil.toBytes(UUID.randomUUID());

    assertAll("database should be empty",
        () -> assertFalse(reportMessageDynamoDb.remove(hash1)),
        () -> assertFalse(reportMessageDynamoDb.remove(hash2))
    );

    reportMessageDynamoDb.store(hash1).join();
    reportMessageDynamoDb.store(hash2).join();

    assertAll("both hashes should be found",
        () -> assertTrue(reportMessageDynamoDb.remove(hash1)),
        () -> assertTrue(reportMessageDynamoDb.remove(hash2))
    );

    assertAll( "database should be empty",
        () -> assertFalse(reportMessageDynamoDb.remove(hash1)),
        () -> assertFalse(reportMessageDynamoDb.remove(hash2))
    );
  }

}
