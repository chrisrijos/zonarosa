package io.zonarosa.messenger.conversation.ui.mentions;

import androidx.annotation.NonNull;

import io.zonarosa.messenger.recipients.Recipient;
import io.zonarosa.messenger.util.viewholders.RecipientMappingModel;

public final class MentionViewState extends RecipientMappingModel<MentionViewState> {

  private final Recipient recipient;

  public MentionViewState(@NonNull Recipient recipient) {
    this.recipient = recipient;
  }

  @Override
  public @NonNull Recipient getRecipient() {
    return recipient;
  }
}
