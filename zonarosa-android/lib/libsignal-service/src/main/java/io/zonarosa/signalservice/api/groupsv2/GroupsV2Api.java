package io.zonarosa.service.api.groupsv2;

import io.zonarosa.libzonarosa.zkgroup.InvalidInputException;
import io.zonarosa.libzonarosa.zkgroup.VerificationFailedException;
import io.zonarosa.libzonarosa.zkgroup.auth.AuthCredentialPresentation;
import io.zonarosa.libzonarosa.zkgroup.auth.AuthCredentialWithPni;
import io.zonarosa.libzonarosa.zkgroup.auth.AuthCredentialWithPniResponse;
import io.zonarosa.libzonarosa.zkgroup.auth.ClientZkAuthOperations;
import io.zonarosa.libzonarosa.zkgroup.calllinks.CallLinkAuthCredentialResponse;
import io.zonarosa.libzonarosa.zkgroup.groups.ClientZkGroupCipher;
import io.zonarosa.libzonarosa.zkgroup.groups.GroupSecretParams;
import io.zonarosa.libzonarosa.zkgroup.groupsend.GroupSendEndorsementsResponse;
import io.zonarosa.storageservice.storage.protos.groups.AvatarUploadAttributes;
import io.zonarosa.storageservice.storage.protos.groups.Group;
import io.zonarosa.storageservice.storage.protos.groups.GroupAttributeBlob;
import io.zonarosa.storageservice.storage.protos.groups.GroupChange;
import io.zonarosa.storageservice.storage.protos.groups.GroupChangeResponse;
import io.zonarosa.storageservice.storage.protos.groups.GroupChanges;
import io.zonarosa.storageservice.storage.protos.groups.ExternalGroupCredential;
import io.zonarosa.storageservice.storage.protos.groups.GroupJoinInfo;
import io.zonarosa.storageservice.storage.protos.groups.GroupResponse;
import io.zonarosa.storageservice.storage.protos.groups.local.DecryptedGroup;
import io.zonarosa.storageservice.storage.protos.groups.local.DecryptedGroupChange;
import io.zonarosa.storageservice.storage.protos.groups.local.DecryptedGroupJoinInfo;
import io.zonarosa.service.api.NetworkResult;
import io.zonarosa.core.models.ServiceId.ACI;
import io.zonarosa.core.models.ServiceId.PNI;
import io.zonarosa.service.api.websocket.ZonaRosaWebSocket;
import io.zonarosa.service.internal.push.PushServiceSocket;
import io.zonarosa.service.internal.push.exceptions.ForbiddenException;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import okio.ByteString;

public class GroupsV2Api {

  private final ZonaRosaWebSocket.AuthenticatedWebSocket authWebSocket;
  private final PushServiceSocket                      socket;
  private final GroupsV2Operations                     groupsOperations;

  public GroupsV2Api(ZonaRosaWebSocket.AuthenticatedWebSocket authWebSocket, PushServiceSocket socket, GroupsV2Operations groupsOperations) {
    this.authWebSocket    = authWebSocket;
    this.socket           = socket;
    this.groupsOperations = groupsOperations;
  }

  /**
   * Provides 7 days of credentials, which you should cache.
   */
  public CredentialResponseMaps getCredentials(long todaySeconds) throws IOException {
    return parseCredentialResponse(GroupsV2ApiHelper.getCredentials(authWebSocket, todaySeconds));
  }

  /**
   * Create an auth token from a credential response.
   */
  public GroupsV2AuthorizationString getGroupsV2AuthorizationString(ACI aci,
                                                                    PNI pni,
                                                                    long redemptionTimeSeconds,
                                                                    GroupSecretParams groupSecretParams,
                                                                    AuthCredentialWithPniResponse authCredentialWithPniResponse)
      throws VerificationFailedException
  {
    ClientZkAuthOperations     authOperations             = groupsOperations.getAuthOperations();
    AuthCredentialWithPni      authCredentialWithPni      = authOperations.receiveAuthCredentialWithPniAsServiceId(aci.getLibZonaRosaAci(), pni.getLibZonaRosaPni(), redemptionTimeSeconds, authCredentialWithPniResponse);
    AuthCredentialPresentation authCredentialPresentation = authOperations.createAuthCredentialPresentation(new SecureRandom(), groupSecretParams, authCredentialWithPni);

    return new GroupsV2AuthorizationString(groupSecretParams, authCredentialPresentation);
  }

  public DecryptedGroupResponse putNewGroup(GroupsV2Operations.NewGroup newGroup,
                                            GroupsV2AuthorizationString authorization)
      throws IOException, InvalidGroupStateException, VerificationFailedException, InvalidInputException
  {
    Group group = newGroup.getNewGroupMessage();

    if (newGroup.getAvatar().isPresent()) {
      String cdnKey = uploadAvatar(newGroup.getAvatar().get(), newGroup.getGroupSecretParams(), authorization);

      group = group.newBuilder()
                   .avatarUrl(cdnKey)
                   .build();
    }

    GroupResponse response = socket.putNewGroupsV2Group(group, authorization);

    return groupsOperations.forGroup(newGroup.getGroupSecretParams())
                           .decryptGroup(Objects.requireNonNull(response.group), response.group_send_endorsements_response.toByteArray());
  }

  public NetworkResult<DecryptedGroupResponse> getGroupAsResult(GroupSecretParams groupSecretParams, GroupsV2AuthorizationString authorization) {
    return NetworkResult.fromFetch(() -> getGroup(groupSecretParams, authorization));
  }

  public DecryptedGroupResponse getGroup(GroupSecretParams groupSecretParams,
                                         GroupsV2AuthorizationString authorization)
      throws IOException, InvalidGroupStateException, VerificationFailedException, InvalidInputException
  {
    GroupResponse response = socket.getGroupsV2Group(authorization);

    return groupsOperations.forGroup(groupSecretParams)
                           .decryptGroup(Objects.requireNonNull(response.group), response.group_send_endorsements_response.toByteArray());
  }

  public GroupHistoryPage getGroupHistoryPage(GroupSecretParams groupSecretParams,
                                              int fromRevision,
                                              GroupsV2AuthorizationString authorization,
                                              boolean includeFirstState,
                                              long sendEndorsementsExpirationMs)
      throws IOException, InvalidGroupStateException, VerificationFailedException, InvalidInputException
  {
    PushServiceSocket.GroupHistory     group           = socket.getGroupHistory(fromRevision, authorization, GroupsV2Operations.HIGHEST_KNOWN_EPOCH, includeFirstState, sendEndorsementsExpirationMs);
    List<DecryptedGroupChangeLog>      result          = new ArrayList<>(group.getGroupChanges().groupChanges.size());
    GroupsV2Operations.GroupOperations groupOperations = groupsOperations.forGroup(groupSecretParams);

    for (GroupChanges.GroupChangeState change : group.getGroupChanges().groupChanges) {
      DecryptedGroup       decryptedGroup  = change.groupState != null ? groupOperations.decryptGroup(change.groupState) : null;
      DecryptedGroupChange decryptedChange = change.groupChange != null ? groupOperations.decryptChange(change.groupChange, DecryptChangeVerificationMode.alreadyTrusted()).orElse(null) : null;

      result.add(new DecryptedGroupChangeLog(decryptedGroup, decryptedChange));
    }

    byte[]                        groupSendEndorsementsResponseBytes = group.getGroupChanges().group_send_endorsements_response.toByteArray();
    GroupSendEndorsementsResponse groupSendEndorsementsResponse      = groupSendEndorsementsResponseBytes.length > 0 ? new GroupSendEndorsementsResponse(groupSendEndorsementsResponseBytes) : null;

    return new GroupHistoryPage(result, groupSendEndorsementsResponse, GroupHistoryPage.PagingData.forGroupHistory(group));
  }

  public NetworkResult<Integer> getGroupJoinedAt(@Nonnull GroupsV2AuthorizationString authorization) {
    return NetworkResult.fromFetch(() -> socket.getGroupJoinedAtRevision(authorization));
  }

  public DecryptedGroupJoinInfo getGroupJoinInfo(GroupSecretParams groupSecretParams,
                                                 Optional<byte[]> password,
                                                 GroupsV2AuthorizationString authorization)
      throws IOException, GroupLinkNotActiveException
  {
    try {
      GroupJoinInfo                      joinInfo        = socket.getGroupJoinInfo(password, authorization);
      GroupsV2Operations.GroupOperations groupOperations = groupsOperations.forGroup(groupSecretParams);

      return groupOperations.decryptGroupJoinInfo(joinInfo);
    } catch (ForbiddenException e) {
      throw new GroupLinkNotActiveException(null, e.getReason());
    }
  }

  public String uploadAvatar(byte[] avatar,
                             GroupSecretParams groupSecretParams,
                             GroupsV2AuthorizationString authorization)
      throws IOException
  {
    AvatarUploadAttributes form = socket.getGroupsV2AvatarUploadForm(authorization.toString());

    byte[] cipherText;
    try {
      cipherText = new ClientZkGroupCipher(groupSecretParams).encryptBlob(new GroupAttributeBlob.Builder().avatar(ByteString.of(avatar)).build().encode());
    } catch (VerificationFailedException e) {
      throw new AssertionError(e);
    }

    socket.uploadGroupV2Avatar(cipherText, form);

    return form.key;
  }

  public GroupChangeResponse patchGroup(GroupChange.Actions groupChange,
                                        GroupsV2AuthorizationString authorization,
                                        Optional<byte[]> groupLinkPassword)
      throws IOException
  {
    return socket.patchGroupsV2Group(groupChange, authorization.toString(), groupLinkPassword);
  }

  public ExternalGroupCredential getExternalGroupCredential(GroupsV2AuthorizationString authorization)
      throws IOException
  {
    return socket.getExternalGroupCredential(authorization);
  }

  private static CredentialResponseMaps parseCredentialResponse(CredentialResponse credentialResponse)
      throws IOException
  {
    HashMap<Long, AuthCredentialWithPniResponse>  credentials         = new HashMap<>();
    HashMap<Long, CallLinkAuthCredentialResponse> callLinkCredentials = new HashMap<>();

    for (TemporalCredential credential : credentialResponse.getCredentials()) {
      AuthCredentialWithPniResponse authCredentialWithPniResponse;
      try {
        authCredentialWithPniResponse = new AuthCredentialWithPniResponse(credential.getCredential());
      } catch (InvalidInputException e) {
        throw new IOException(e);
      }

      credentials.put(credential.getRedemptionTime(), authCredentialWithPniResponse);
    }

    for (TemporalCredential credential : credentialResponse.getCallLinkAuthCredentials()) {
      CallLinkAuthCredentialResponse callLinkAuthCredentialResponse;
      try {
        callLinkAuthCredentialResponse = new CallLinkAuthCredentialResponse(credential.getCredential());
      } catch (InvalidInputException e) {
        throw new IOException(e);
      }

      callLinkCredentials.put(credential.getRedemptionTime(), callLinkAuthCredentialResponse);
    }

    return new CredentialResponseMaps(credentials, callLinkCredentials);
  }

  public static class CredentialResponseMaps {
    private final Map<Long, AuthCredentialWithPniResponse>  authCredentialWithPniResponseHashMap;
    private final Map<Long, CallLinkAuthCredentialResponse> callLinkAuthCredentialResponseHashMap;

    public CredentialResponseMaps(Map<Long, AuthCredentialWithPniResponse> authCredentialWithPniResponseHashMap,
                                  Map<Long, CallLinkAuthCredentialResponse> callLinkAuthCredentialResponseHashMap)
    {
      this.authCredentialWithPniResponseHashMap  = authCredentialWithPniResponseHashMap;
      this.callLinkAuthCredentialResponseHashMap = callLinkAuthCredentialResponseHashMap;
    }

    public Map<Long, AuthCredentialWithPniResponse> getAuthCredentialWithPniResponseHashMap() {
      return authCredentialWithPniResponseHashMap;
    }

    public Map<Long, CallLinkAuthCredentialResponse> getCallLinkAuthCredentialResponseHashMap() {
      return callLinkAuthCredentialResponseHashMap;
    }

    public CredentialResponseMaps createUnmodifiableCopy() {
      return new CredentialResponseMaps(
          Map.copyOf(authCredentialWithPniResponseHashMap),
          Map.copyOf(callLinkAuthCredentialResponseHashMap)
      );
    }
  }
}
