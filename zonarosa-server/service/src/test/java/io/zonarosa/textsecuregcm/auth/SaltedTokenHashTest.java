/*
 * Copyright 2013 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.auth;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.jupiter.api.Test;

class SaltedTokenHashTest {

  @Test
  void testCreating() {
    SaltedTokenHash credentials = SaltedTokenHash.generateFor("mypassword");
    assertThat(credentials.salt()).isNotEmpty();
    assertThat(credentials.hash()).isNotEmpty();
    assertThat(credentials.hash().length()).isEqualTo(66);
  }

  @Test
  void testMatching() {
    SaltedTokenHash credentials = SaltedTokenHash.generateFor("mypassword");

    SaltedTokenHash provided = new SaltedTokenHash(credentials.hash(), credentials.salt());
    assertThat(provided.verify("mypassword")).isTrue();
  }

  @Test
  void testMisMatching() {
    SaltedTokenHash credentials = SaltedTokenHash.generateFor("mypassword");

    SaltedTokenHash provided = new SaltedTokenHash(credentials.hash(), credentials.salt());
    assertThat(provided.verify("wrong")).isFalse();
  }

}
