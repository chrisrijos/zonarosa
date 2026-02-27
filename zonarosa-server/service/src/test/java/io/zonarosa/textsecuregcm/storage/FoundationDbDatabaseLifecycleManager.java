/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.storage;

import com.apple.foundationdb.Database;
import com.apple.foundationdb.FDB;
import java.io.IOException;

interface FoundationDbDatabaseLifecycleManager {

  void initializeDatabase(final FDB fdb) throws IOException;

  Database getDatabase();

  void closeDatabase();
}
