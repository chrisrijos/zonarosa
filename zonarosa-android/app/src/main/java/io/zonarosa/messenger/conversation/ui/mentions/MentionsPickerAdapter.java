package io.zonarosa.messenger.conversation.ui.mentions;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.zonarosa.messenger.R;
import io.zonarosa.messenger.util.adapter.mapping.MappingAdapter;
import io.zonarosa.messenger.util.adapter.mapping.MappingModel;
import io.zonarosa.messenger.util.viewholders.RecipientViewHolder;
import io.zonarosa.messenger.util.viewholders.RecipientViewHolder.EventListener;

import java.util.List;

public class MentionsPickerAdapter extends MappingAdapter {
  private final Runnable currentListChangedListener;

  public MentionsPickerAdapter(@Nullable EventListener<MentionViewState> listener, @NonNull Runnable currentListChangedListener) {
    this.currentListChangedListener = currentListChangedListener;
    registerFactory(MentionViewState.class, RecipientViewHolder.createFactory(R.layout.mentions_picker_recipient_list_item, listener));
  }

  @Override
  public void onCurrentListChanged(@NonNull List<MappingModel<?>> previousList, @NonNull List<MappingModel<?>> currentList) {
    super.onCurrentListChanged(previousList, currentList);
    currentListChangedListener.run();
  }
}
