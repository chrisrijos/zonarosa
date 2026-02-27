package io.zonarosa.service.api.storage;

import org.junit.Test;
import io.zonarosa.core.models.ServiceId.ACI;
import io.zonarosa.service.internal.storage.protos.ContactRecord;

import okio.ByteString;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class ZonaRosaContactRecordTest {

  private static final ACI    ACI_A  = ACI.parseOrThrow("ebef429e-695e-4f51-bcc4-526a60ac68c7");
  private static final String E164_A = "+16108675309";

  @Test
  public void contacts_with_same_identity_key_contents_are_equal() {
    byte[] identityKey     = new byte[32];
    byte[] identityKeyCopy = identityKey.clone();

    ContactRecord contactA = contactBuilder(ACI_A, E164_A, "a").identityKey(ByteString.of(identityKey)).build();
    ContactRecord contactB = contactBuilder(ACI_A, E164_A, "a").identityKey(ByteString.of(identityKeyCopy)).build();

    ZonaRosaContactRecord zonarosaContactA = new ZonaRosaContactRecord(StorageId.forContact(byteArray(1)), contactA);
    ZonaRosaContactRecord zonarosaContactB = new ZonaRosaContactRecord(StorageId.forContact(byteArray(1)), contactB);

    assertEquals(zonarosaContactA, zonarosaContactB);
    assertEquals(zonarosaContactA.hashCode(), zonarosaContactB.hashCode());
  }

  @Test
  public void contacts_with_different_identity_key_contents_are_not_equal() {
    byte[] identityKey     = new byte[32];
    byte[] identityKeyCopy = identityKey.clone();
    identityKeyCopy[0] = 1;

    ContactRecord contactA = contactBuilder(ACI_A, E164_A, "a").identityKey(ByteString.of(identityKey)).build();
    ContactRecord contactB = contactBuilder(ACI_A, E164_A, "a").identityKey(ByteString.of(identityKeyCopy)).build();

    ZonaRosaContactRecord zonarosaContactA = new ZonaRosaContactRecord(StorageId.forContact(byteArray(1)), contactA);
    ZonaRosaContactRecord zonarosaContactB = new ZonaRosaContactRecord(StorageId.forContact(byteArray(1)), contactB);

    assertNotEquals(zonarosaContactA, zonarosaContactB);
    assertNotEquals(zonarosaContactA.hashCode(), zonarosaContactB.hashCode());
  }

  private static byte[] byteArray(int a) {
    byte[] bytes = new byte[4];
    bytes[3] = (byte) a;
    bytes[2] = (byte)(a >> 8);
    bytes[1] = (byte)(a >> 16);
    bytes[0] = (byte)(a >> 24);
    return bytes;
  }

  private static ContactRecord.Builder contactBuilder(ACI serviceId, String e164, String givenName) {
    return new ContactRecord.Builder()
        .e164(e164)
        .givenName(givenName);
  }
}
