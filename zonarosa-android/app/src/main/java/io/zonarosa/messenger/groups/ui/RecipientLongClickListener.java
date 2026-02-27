package io.zonarosa.messenger.groups.ui;

import androidx.annotation.NonNull;

import io.zonarosa.messenger.recipients.Recipient;

public interface RecipientLongClickListener {
  boolean onLongClick(@NonNull Recipient recipient);
}
