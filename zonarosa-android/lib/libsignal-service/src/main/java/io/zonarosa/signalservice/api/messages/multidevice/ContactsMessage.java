package io.zonarosa.service.api.messages.multidevice;


import io.zonarosa.service.api.messages.ZonaRosaServiceAttachment;

public class ContactsMessage {

  private final ZonaRosaServiceAttachment contacts;
  private final boolean                 complete;

  public ContactsMessage(ZonaRosaServiceAttachment contacts, boolean complete) {
    this.contacts = contacts;
    this.complete = complete;
  }

  public ZonaRosaServiceAttachment getContactsStream() {
    return contacts;
  }

  public boolean isComplete() {
    return complete;
  }
}
