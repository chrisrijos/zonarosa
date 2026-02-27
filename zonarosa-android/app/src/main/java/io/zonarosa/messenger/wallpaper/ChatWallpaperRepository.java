package io.zonarosa.messenger.wallpaper;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import io.zonarosa.core.util.concurrent.ZonaRosaExecutors;
import io.zonarosa.messenger.conversation.colors.ChatColors;
import io.zonarosa.messenger.conversation.colors.ChatColorsPalette;
import io.zonarosa.messenger.database.ZonaRosaDatabase;
import io.zonarosa.messenger.keyvalue.ZonaRosaStore;
import io.zonarosa.messenger.recipients.Recipient;
import io.zonarosa.messenger.recipients.RecipientId;
import io.zonarosa.messenger.util.concurrent.SerialExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;

class ChatWallpaperRepository {

  private static final Executor EXECUTOR = new SerialExecutor(ZonaRosaExecutors.BOUNDED);

  @MainThread
  @Nullable ChatWallpaper getCurrentWallpaper(@Nullable RecipientId recipientId) {
    if (recipientId != null) {
      return Recipient.live(recipientId).get().getWallpaper();
    } else {
      return ZonaRosaStore.wallpaper().getWallpaper();
    }
  }

  @MainThread
  @NonNull ChatColors getCurrentChatColors(@Nullable RecipientId recipientId) {
    if (recipientId != null) {
      return Recipient.live(recipientId).get().getChatColors();
    } else if (ZonaRosaStore.chatColors().hasChatColors()) {
      return Objects.requireNonNull(ZonaRosaStore.chatColors().getChatColors());
    } else if (ZonaRosaStore.wallpaper().hasWallpaperSet()) {
      return Objects.requireNonNull(ZonaRosaStore.wallpaper().getWallpaper()).getAutoChatColors();
    } else {
      return ChatColorsPalette.Bubbles.getDefault().withId(ChatColors.Id.Auto.INSTANCE);
    }
  }

  void getAllWallpaper(@NonNull Consumer<List<ChatWallpaper>> consumer) {
    EXECUTOR.execute(() -> {
      List<ChatWallpaper> wallpapers = new ArrayList<>(ChatWallpaper.BuiltIns.INSTANCE.getAllBuiltIns());

      wallpapers.addAll(WallpaperStorage.getAll());
      consumer.accept(wallpapers);
    });
  }

  void saveWallpaper(@Nullable RecipientId recipientId, @Nullable ChatWallpaper chatWallpaper, @NonNull Runnable onWallpaperSaved) {
    EXECUTOR.execute(() -> {
      if (recipientId != null) {
        //noinspection CodeBlock2Expr
        ZonaRosaDatabase.recipients().setWallpaper(recipientId, chatWallpaper, true);
        onWallpaperSaved.run();
      } else {
        ZonaRosaStore.wallpaper().setWallpaper(chatWallpaper);
        onWallpaperSaved.run();
      }
    });
  }

  void resetAllWallpaper(@NonNull Runnable onWallpaperReset) {
    EXECUTOR.execute(() -> {
      ZonaRosaStore.wallpaper().setWallpaper(null);
      ZonaRosaDatabase.recipients().resetAllWallpaper();
      onWallpaperReset.run();
    });
  }

  void resetAllChatColors(@NonNull Runnable onColorsReset) {
    ZonaRosaStore.chatColors().setChatColors(null);
    EXECUTOR.execute(() -> {
      ZonaRosaDatabase.recipients().clearAllColors();
      onColorsReset.run();
    });
  }

  void setDimInDarkTheme(@Nullable RecipientId recipientId, boolean dimInDarkTheme) {
    if (recipientId != null) {
      EXECUTOR.execute(() -> {
        Recipient recipient = Recipient.resolved(recipientId);
        if (recipient.getHasOwnWallpaper()) {
          ZonaRosaDatabase.recipients().setDimWallpaperInDarkTheme(recipientId, dimInDarkTheme);
        } else if (recipient.getHasWallpaper()) {
          ZonaRosaDatabase.recipients()
                       .setWallpaper(recipientId,
                                     ChatWallpaperFactory.updateWithDimming(recipient.getWallpaper(),
                                                                            dimInDarkTheme ? ChatWallpaper.FIXED_DIM_LEVEL_FOR_DARK_THEME
                                                                                           : 0f),
                                     false);
        } else {
          throw new IllegalStateException("Unexpected call to setDimInDarkTheme, no wallpaper has been set on the given recipient or globally.");
        }
      });
    } else {
      ZonaRosaStore.wallpaper().setDimInDarkTheme(dimInDarkTheme);
    }
  }

  public void clearChatColor(@Nullable RecipientId recipientId, @NonNull Runnable onChatColorCleared) {
    if (recipientId == null) {
      ZonaRosaStore.chatColors().setChatColors(null);
      onChatColorCleared.run();
    } else {
      EXECUTOR.execute(() -> {
        ZonaRosaDatabase.recipients().clearColor(recipientId);
        onChatColorCleared.run();
      });
    }
  }
}
