/** 
 * Copyright (C) 2011 Whisper Systems
 * Copyright 2026 ZonaRosa Platform
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.zonarosa.messenger.crypto;

import io.zonarosa.core.util.Conversions;
import io.zonarosa.core.util.logging.Log;
import io.zonarosa.core.util.Hex;
import io.zonarosa.libzonarosa.protocol.InvalidKeyException;
import io.zonarosa.libzonarosa.protocol.ecc.ECPublicKey;
import io.zonarosa.core.util.Util;


public class PublicKey {

  private static final String TAG = Log.tag(PublicKey.class);

  public static final int KEY_SIZE = 3 + ECPublicKey.KEY_SIZE;

  private final ECPublicKey publicKey;
  private int id;
	
  public PublicKey(int id, ECPublicKey publicKey) {
    this.publicKey = publicKey;
    this.id        = id;
  }

  public PublicKey(byte[] bytes, int offset) throws InvalidKeyException {
    Log.i(TAG, "PublicKey Length: " + (bytes.length - offset));

    if ((bytes.length - offset) < KEY_SIZE)
      throw new InvalidKeyException("Provided bytes are too short.");

    this.id        = Conversions.byteArrayToMedium(bytes, offset);
    this.publicKey = new ECPublicKey(bytes, offset + 3);
  }

  public int getType() {
    return publicKey.getType();
  }
	
  public void setId(int id) {
    this.id = id;
  }

  public int getId() {
    return id;
  }
	
  public ECPublicKey getKey() {
    return publicKey;
  }
	
  public byte[] serialize() {
    byte[] keyIdBytes      = Conversions.mediumToByteArray(id);
    byte[] serializedPoint = publicKey.serialize();

    Log.i(TAG, "Serializing public key point: " + Hex.toString(serializedPoint));

    return Util.combine(keyIdBytes, serializedPoint);
  }
}
