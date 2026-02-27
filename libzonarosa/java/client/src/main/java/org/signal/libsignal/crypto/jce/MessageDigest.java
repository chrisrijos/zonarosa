//
// Copyright 2021 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.crypto.jce;

import java.security.NoSuchAlgorithmException;
import io.zonarosa.libzonarosa.crypto.CryptographicHash;

public class MessageDigest {
  CryptographicHash hash;

  public static MessageDigest getInstance(String algoName) throws NoSuchAlgorithmException {
    return new MessageDigest(algoName);
  }

  private MessageDigest(String algoName) throws NoSuchAlgorithmException {
    this.hash = new CryptographicHash(algoName);
  }

  public void update(byte[] input, int offset, int len) {
    this.hash.update(input, offset, len);
  }

  public void update(byte[] input) {
    update(input, 0, input.length);
  }

  public byte[] doFinal() {
    return this.hash.finish();
  }

  public byte[] doFinal(byte[] last) {
    update(last);
    return doFinal();
  }

  public byte[] doFinal(byte[] last, int offset, int len) {
    update(last, offset, len);
    return doFinal();
  }
}
