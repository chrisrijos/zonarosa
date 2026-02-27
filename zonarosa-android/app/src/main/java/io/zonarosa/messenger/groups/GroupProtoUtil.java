package io.zonarosa.messenger.groups;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import io.zonarosa.libzonarosa.zkgroup.groups.GroupMasterKey;
import io.zonarosa.storageservice.storage.protos.groups.GroupChange;
import io.zonarosa.storageservice.storage.protos.groups.local.DecryptedGroup;
import io.zonarosa.storageservice.storage.protos.groups.local.DecryptedGroupChange;
import io.zonarosa.storageservice.storage.protos.groups.local.DecryptedMember;
import io.zonarosa.storageservice.storage.protos.groups.local.DecryptedPendingMember;
import io.zonarosa.messenger.backup.v2.proto.GroupChangeChatUpdate;
import io.zonarosa.messenger.database.model.GroupsV2UpdateMessageConverter;
import io.zonarosa.messenger.database.model.databaseprotos.DecryptedGroupV2Context;
import io.zonarosa.messenger.database.model.databaseprotos.GV2UpdateDescription;
import io.zonarosa.messenger.keyvalue.ZonaRosaStore;
import io.zonarosa.messenger.recipients.Recipient;
import io.zonarosa.messenger.recipients.RecipientId;
import io.zonarosa.core.models.ServiceId;
import io.zonarosa.core.models.ServiceId.ACI;
import io.zonarosa.service.internal.push.GroupContextV2;

import java.util.List;

import okio.ByteString;

public final class GroupProtoUtil {

  private GroupProtoUtil() {
  }

  public static GV2UpdateDescription createOutgoingGroupV2UpdateDescription(@NonNull GroupMasterKey masterKey,
                                                                            @NonNull GroupMutation groupMutation,
                                                                            @Nullable GroupChange signedServerChange)
  {
    DecryptedGroupV2Context groupV2Context = createDecryptedGroupV2Context(masterKey, groupMutation, signedServerChange);
    GroupChangeChatUpdate   update         = GroupsV2UpdateMessageConverter.translateDecryptedChange(ZonaRosaStore.account().getServiceIds(), groupV2Context);

    return new GV2UpdateDescription.Builder()
        .gv2ChangeDescription(groupV2Context)
        .groupChangeUpdate(update)
        .build();
  }

  public static DecryptedGroupV2Context createDecryptedGroupV2Context(@NonNull GroupMasterKey masterKey,
                                                                      @NonNull GroupMutation groupMutation,
                                                                      @Nullable GroupChange signedServerChange)
  {
    DecryptedGroupChange   plainGroupChange = groupMutation.getGroupChange();
    DecryptedGroup         decryptedGroup   = groupMutation.getNewGroupState();
    int                    revision         = plainGroupChange != null ? plainGroupChange.revision : decryptedGroup.revision;
    GroupContextV2.Builder contextBuilder   = new GroupContextV2.Builder()
                                                                .masterKey(ByteString.of(masterKey.serialize()))
                                                                .revision(revision);

    if (signedServerChange != null) {
      contextBuilder.groupChange(signedServerChange.encodeByteString());
    }

    DecryptedGroupV2Context.Builder builder = new DecryptedGroupV2Context.Builder()
                                                                         .context(contextBuilder.build())
                                                                         .groupState(decryptedGroup);

    if (groupMutation.getPreviousGroupState() != null) {
      builder.previousGroupState(groupMutation.getPreviousGroupState());
    }

    if (plainGroupChange != null) {
      builder.change(plainGroupChange);
    }

    return builder.build();
  }

  @WorkerThread
  public static Recipient pendingMemberToRecipient(@NonNull DecryptedPendingMember pendingMember) {
    return pendingMemberServiceIdToRecipient(pendingMember.serviceIdBytes);
  }

  @WorkerThread
  public static Recipient pendingMemberServiceIdToRecipient(@NonNull ByteString serviceIdBinary) {
    ServiceId serviceId = ServiceId.parseOrThrow(serviceIdBinary);

    if (serviceId.isUnknown()) {
      return Recipient.UNKNOWN;
    }

    return Recipient.externalPush(serviceId);
  }

  @WorkerThread
  public static @NonNull RecipientId serviceIdBinaryToRecipientId(@NonNull ByteString serviceIdBinary) {
    ServiceId serviceId = ServiceId.parseOrThrow(serviceIdBinary);

    if (serviceId.isUnknown()) {
      return RecipientId.UNKNOWN;
    }

    return RecipientId.from(serviceId);
  }

  public static boolean isMember(@NonNull ACI aci, @NonNull List<DecryptedMember> membersList) {
    ByteString aciBytes = aci.toByteString();

    for (DecryptedMember member : membersList) {
      if (aciBytes.equals(member.aciBytes)) {
        return true;
      }
    }

    return false;
  }
}
