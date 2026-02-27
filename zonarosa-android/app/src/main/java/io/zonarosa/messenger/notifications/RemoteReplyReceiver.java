/*
 * Copyright (C) 2016 ZonaRosa Systems
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

package io.zonarosa.messenger.notifications;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.core.app.RemoteInput;

import io.zonarosa.core.util.concurrent.ZonaRosaExecutors;
import io.zonarosa.messenger.database.MessageTable.MarkedMessageInfo;
import io.zonarosa.messenger.database.ZonaRosaDatabase;
import io.zonarosa.messenger.database.model.ParentStoryId;
import io.zonarosa.messenger.dependencies.AppDependencies;
import io.zonarosa.messenger.mms.OutgoingMessage;
import io.zonarosa.messenger.mms.SlideDeck;
import io.zonarosa.messenger.notifications.v2.ConversationId;
import io.zonarosa.messenger.notifications.v2.DefaultMessageNotifier;
import io.zonarosa.messenger.recipients.Recipient;
import io.zonarosa.messenger.recipients.RecipientId;
import io.zonarosa.messenger.sms.MessageSender;
import io.zonarosa.messenger.util.MessageUtil;

import java.util.List;

/**
 * Get the response text from the Wearable Device and sends an message as a reply
 */
public class RemoteReplyReceiver extends BroadcastReceiver {

  public static final String REPLY_ACTION         = "io.zonarosa.messenger.notifications.WEAR_REPLY";
  public static final String RECIPIENT_EXTRA      = "recipient_extra";
  public static final String REPLY_METHOD         = "reply_method";
  public static final String EARLIEST_TIMESTAMP   = "earliest_timestamp";
  public static final String GROUP_STORY_ID_EXTRA = "group_story_id_extra";

  @SuppressLint("StaticFieldLeak")
  @Override
  public void onReceive(final Context context, Intent intent) {
    if (!REPLY_ACTION.equals(intent.getAction())) return;

    Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);

    if (remoteInput == null) return;

    final RecipientId  recipientId  = intent.getParcelableExtra(RECIPIENT_EXTRA);
    final ReplyMethod  replyMethod  = (ReplyMethod) intent.getSerializableExtra(REPLY_METHOD);
    final CharSequence responseText = remoteInput.getCharSequence(DefaultMessageNotifier.EXTRA_REMOTE_REPLY);
    final long         groupStoryId = intent.getLongExtra(GROUP_STORY_ID_EXTRA, Long.MIN_VALUE);

    if (recipientId == null) throw new AssertionError("No recipientId specified");
    if (replyMethod == null) throw new AssertionError("No reply method specified");

    if (responseText != null) {
      ZonaRosaExecutors.BOUNDED.execute(() -> {
        long threadId;

        Recipient               recipient     = Recipient.resolved(recipientId);
        String                  body          = responseText.toString();
        ParentStoryId           parentStoryId = groupStoryId != Long.MIN_VALUE ? ParentStoryId.deserialize(groupStoryId) : null;
        MessageUtil.SplitResult splitMessage  = MessageUtil.getSplitMessage(context, body);
        SlideDeck               slideDeck     = null;

        if (splitMessage.getTextSlide().isPresent()) {
          slideDeck = new SlideDeck();
          slideDeck.addSlide(splitMessage.getTextSlide().get());
        }

        switch (replyMethod) {
          case SecureMessage:
          case GroupMessage: {
            OutgoingMessage reply = OutgoingMessage.quickReply(recipient,
                                                               slideDeck,
                                                               splitMessage.getBody(),
                                                               parentStoryId);

            threadId = MessageSender.send(context, reply, -1, MessageSender.SendType.ZONAROSA, null, null);
            break;
          }
          default:
            throw new AssertionError("Unknown Reply method");
        }

        AppDependencies.getMessageNotifier()
                       .addStickyThread(new ConversationId(threadId, groupStoryId != Long.MIN_VALUE ? groupStoryId : null),
                                                intent.getLongExtra(EARLIEST_TIMESTAMP, System.currentTimeMillis()));

        List<MarkedMessageInfo> messageIds = ZonaRosaDatabase.threads().setRead(threadId);

        AppDependencies.getMessageNotifier().updateNotification(context);
        MarkReadReceiver.process(messageIds);
      });
    }
  }
}
