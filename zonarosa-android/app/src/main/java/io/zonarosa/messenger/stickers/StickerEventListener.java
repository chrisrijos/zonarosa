package io.zonarosa.messenger.stickers;

import androidx.annotation.NonNull;

import io.zonarosa.messenger.database.model.StickerRecord;

public interface StickerEventListener {
  void onStickerSelected(@NonNull StickerRecord sticker);

  void onStickerManagementClicked();
}
