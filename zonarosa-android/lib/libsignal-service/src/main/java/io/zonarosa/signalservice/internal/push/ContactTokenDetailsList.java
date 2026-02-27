/**
 * Copyright (C) 2014-2016 ZonaRosa Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */

package io.zonarosa.service.internal.push;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.zonarosa.service.api.push.ContactTokenDetails;

import java.util.List;

public class ContactTokenDetailsList {

  @JsonProperty
  private List<ContactTokenDetails> contacts;

  public ContactTokenDetailsList() {}

  public List<ContactTokenDetails> getContacts() {
    return contacts;
  }
}
