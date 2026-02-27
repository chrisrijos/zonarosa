/*
 * Copyright 2013-2021 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.spam;


import io.zonarosa.server.storage.Account;
import java.io.IOException;

public interface RateLimitChallengeListener {

  void handleRateLimitChallengeAnswered(Account account, ChallengeType type);
}
