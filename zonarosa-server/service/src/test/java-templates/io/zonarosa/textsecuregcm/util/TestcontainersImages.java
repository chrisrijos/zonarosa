/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.util;

public class TestcontainersImages {

  private static final String DYNAMO_DB = "${dynamodb.image}";
  private static final String LOCAL_STACK = "${localstack.image}";
  private static final String REDIS = "${redis.image}";
  private static final String REDIS_CLUSTER = "${redis-cluster.image}";

  public static String getDynamoDb() {
    return DYNAMO_DB;
  }

  public static String getLocalStack() {
    return LOCAL_STACK;
  }

  public static String getRedis() {
    return REDIS;
  }

  public static String getRedisCluster() {
    return REDIS_CLUSTER;
  }
}
