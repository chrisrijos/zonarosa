package io.zonarosa.messenger.reactions.any;

import android.content.Context;

import androidx.annotation.NonNull;

import com.annimon.stream.Stream;

import io.zonarosa.core.util.ThreadUtil;
import io.zonarosa.core.util.concurrent.ZonaRosaExecutors;
import io.zonarosa.core.util.logging.Log;
import io.zonarosa.messenger.R;
import io.zonarosa.messenger.components.emoji.RecentEmojiPageModel;
import io.zonarosa.messenger.database.ZonaRosaDatabase;
import io.zonarosa.messenger.database.model.MessageId;
import io.zonarosa.messenger.database.model.ReactionRecord;
import io.zonarosa.messenger.emoji.EmojiCategory;
import io.zonarosa.messenger.emoji.EmojiSource;
import io.zonarosa.messenger.reactions.ReactionDetails;
import io.zonarosa.messenger.recipients.Recipient;
import io.zonarosa.messenger.sms.MessageSender;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

final class ReactWithAnyEmojiRepository {

  private static final String TAG = Log.tag(ReactWithAnyEmojiRepository.class);

  private final Context                     context;
  private final RecentEmojiPageModel        recentEmojiPageModel;
  private final List<ReactWithAnyEmojiPage> emojiPages;

  ReactWithAnyEmojiRepository(@NonNull Context context, @NonNull String storageKey) {
    this.context              = context;
    this.recentEmojiPageModel = new RecentEmojiPageModel(context, storageKey);
    this.emojiPages           = new LinkedList<>();

    emojiPages.addAll(Stream.of(EmojiSource.getLatest().getDisplayPages())
                            .filterNot(p -> p.getIconAttr() == EmojiCategory.EMOTICONS.getIcon())
                            .map(page -> new ReactWithAnyEmojiPage(Collections.singletonList(new ReactWithAnyEmojiPageBlock(EmojiCategory.getCategoryLabel(page.getIconAttr()), page))))
                            .toList());
  }

  List<ReactWithAnyEmojiPage> getEmojiPageModels(@NonNull List<ReactionDetails> thisMessagesReactions) {
    List<ReactWithAnyEmojiPage> pages       = new LinkedList<>();
    List<String>                thisMessage = Stream.of(thisMessagesReactions)
                                                    .map(ReactionDetails::getDisplayEmoji)
                                                    .distinct()
                                                    .toList();

    if (thisMessage.isEmpty()) {
      pages.add(new ReactWithAnyEmojiPage(Collections.singletonList(new ReactWithAnyEmojiPageBlock(R.string.ReactWithAnyEmojiBottomSheetDialogFragment__recently_used, recentEmojiPageModel))));
    } else {
      pages.add(new ReactWithAnyEmojiPage(Arrays.asList(new ReactWithAnyEmojiPageBlock(R.string.ReactWithAnyEmojiBottomSheetDialogFragment__this_message, new ThisMessageEmojiPageModel(thisMessage)),
                                                        new ReactWithAnyEmojiPageBlock(R.string.ReactWithAnyEmojiBottomSheetDialogFragment__recently_used, recentEmojiPageModel))));
    }

    pages.addAll(emojiPages);

    return pages;
  }

  void addEmojiToMessage(@NonNull String emoji, @NonNull MessageId messageId) {
    ZonaRosaExecutors.BOUNDED.execute(() -> {
      ReactionRecord  oldRecord = Stream.of(ZonaRosaDatabase.reactions().getReactions(messageId))
                                        .filter(record -> record.getAuthor().equals(Recipient.self().getId()))
                                        .findFirst()
                                        .orElse(null);

      if (oldRecord != null && oldRecord.getEmoji().equals(emoji)) {
        MessageSender.sendReactionRemoval(context, messageId, oldRecord);
      } else {
        MessageSender.sendNewReaction(context, messageId, emoji);
        ThreadUtil.runOnMain(() -> recentEmojiPageModel.onCodePointSelected(emoji));
      }
    });
  }
}
