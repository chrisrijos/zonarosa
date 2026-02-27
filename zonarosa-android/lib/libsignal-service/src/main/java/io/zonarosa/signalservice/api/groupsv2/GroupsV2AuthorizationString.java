package io.zonarosa.service.api.groupsv2;

import io.zonarosa.core.util.Hex;
import io.zonarosa.libzonarosa.zkgroup.auth.AuthCredentialPresentation;
import io.zonarosa.libzonarosa.zkgroup.groups.GroupSecretParams;

import okhttp3.Credentials;

public final class GroupsV2AuthorizationString {

  private final String authString;

  GroupsV2AuthorizationString(GroupSecretParams groupSecretParams, AuthCredentialPresentation authCredentialPresentation) {
    String username = Hex.toStringCondensed(groupSecretParams.getPublicParams().serialize());
    String password = Hex.toStringCondensed(authCredentialPresentation.serialize());

    authString = Credentials.basic(username, password);
  }

  @Override
  public String toString() {
    return authString;
  }
}
