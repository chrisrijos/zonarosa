package io.zonarosa.messenger.util;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import io.zonarosa.core.util.BidiUtil;
import io.zonarosa.core.util.logging.Log;
import io.zonarosa.libzonarosa.zkgroup.InvalidInputException;
import io.zonarosa.libzonarosa.zkgroup.groups.GroupMasterKey;
import io.zonarosa.messenger.R;
import io.zonarosa.messenger.database.GroupTable;
import io.zonarosa.messenger.database.ZonaRosaDatabase;
import io.zonarosa.messenger.database.model.GroupRecord;
import io.zonarosa.messenger.groups.GroupId;
import io.zonarosa.messenger.messages.ZonaRosaServiceProtoUtil;
import io.zonarosa.messenger.mms.MessageGroupContext;
import io.zonarosa.messenger.recipients.Recipient;
import io.zonarosa.messenger.recipients.RecipientId;
import io.zonarosa.service.api.messages.ZonaRosaServiceDataMessage;
import io.zonarosa.service.api.messages.ZonaRosaServiceGroupV2;
import io.zonarosa.service.internal.push.Content;
import io.zonarosa.service.internal.push.GroupContextV2;

import java.io.IOException;
import java.util.List;

public final class GroupUtil {

  private GroupUtil() {
  }

  private static final String TAG = Log.tag(GroupUtil.class);

  public static @Nullable GroupContextV2 getGroupContextIfPresent(@NonNull Content content) {
    if (content.dataMessage != null && ZonaRosaServiceProtoUtil.INSTANCE.getHasGroupContext(content.dataMessage)) {
      return content.dataMessage.groupV2;
    } else if (content.syncMessage != null                 &&
               content.syncMessage.sent != null &&
               content.syncMessage.sent.message != null &&
               ZonaRosaServiceProtoUtil.INSTANCE.getHasGroupContext(content.syncMessage.sent.message))
    {
      return content.syncMessage.sent.message.groupV2;
    } else if (content.storyMessage != null && ZonaRosaServiceProtoUtil.INSTANCE.isValid(content.storyMessage.group)) {
      return content.storyMessage.group;
    } else {
      return null;
    }
  }

  public static @NonNull GroupMasterKey requireMasterKey(@NonNull byte[] masterKey) {
    try {
      return new GroupMasterKey(masterKey);
    } catch (InvalidInputException e) {
      throw new AssertionError(e);
    }
  }

  public static @NonNull GroupDescription getNonV2GroupDescription(@NonNull Context context, @Nullable String encodedGroup) {
    if (encodedGroup == null) {
      return new GroupDescription(context, null);
    }

    try {
      MessageGroupContext groupContext = new MessageGroupContext(encodedGroup, false);
      return new GroupDescription(context, groupContext);
    } catch (IOException e) {
      Log.w(TAG, e);
      return new GroupDescription(context, null);
    }
  }

  @WorkerThread
  public static void setDataMessageGroupContext(@NonNull Context context,
                                                @NonNull ZonaRosaServiceDataMessage.Builder dataMessageBuilder,
                                                @NonNull GroupId.Push groupId)
  {
    if (groupId.isV2()) {
      GroupTable                   groupDatabase     = ZonaRosaDatabase.groups();
      GroupRecord                  groupRecord       = groupDatabase.requireGroup(groupId);
      GroupTable.V2GroupProperties v2GroupProperties = groupRecord.requireV2GroupProperties();
      ZonaRosaServiceGroupV2            group             = ZonaRosaServiceGroupV2.newBuilder(v2GroupProperties.getGroupMasterKey())
                                                                              .withRevision(v2GroupProperties.getGroupRevision())
                                                                              .build();
      dataMessageBuilder.asGroupMessage(group);
    }
  }

  public static class GroupDescription {

    @NonNull  private final Context             context;
    @Nullable private final MessageGroupContext groupContext;
    @Nullable private final List<RecipientId>   members;

    GroupDescription(@NonNull Context context, @Nullable MessageGroupContext groupContext) {
      this.context      = context.getApplicationContext();
      this.groupContext = groupContext;

      if (groupContext == null) {
        this.members = null;
      } else {
        List<RecipientId> membersList = groupContext.getMembersListExcludingSelf();
        this.members = membersList.isEmpty() ? null : membersList;
      }
    }

    @WorkerThread
    public String toString(@NonNull Recipient sender) {
      StringBuilder description = new StringBuilder();
      description.append(context.getString(R.string.MessageRecord_s_updated_group, sender.getDisplayName(context)));

      if (groupContext == null) {
        return description.toString();
      }

      String title = BidiUtil.isolateBidi(groupContext.getName());

      if (members != null && members.size() > 0) {
        description.append("\n");
        description.append(context.getResources().getQuantityString(R.plurals.GroupUtil_joined_the_group,
                                                                    members.size(), toString(members)));
      }

      if (!title.trim().isEmpty()) {
        if (members != null) description.append(" ");
        else                 description.append("\n");
        description.append(context.getString(R.string.GroupUtil_group_name_is_now, title));
      }

      return description.toString();
    }

    private String toString(List<RecipientId> recipients) {
      StringBuilder result = new StringBuilder();

      for (int i = 0; i < recipients.size(); i++) {
        result.append(Recipient.live(recipients.get(i)).get().getDisplayName(context));

      if (i != recipients.size() -1 )
        result.append(", ");
    }

    return result.toString();
    }
  }
}
