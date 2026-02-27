package io.zonarosa.messenger.groups.ui;

import androidx.annotation.NonNull;

import io.zonarosa.messenger.recipients.Recipient;

public interface RecipientClickListener {
  void onClick(@NonNull Recipient recipient);
}
