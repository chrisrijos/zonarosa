package io.zonarosa.messenger.messagedetails;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.zonarosa.messenger.database.documents.IdentityKeyMismatch;
import io.zonarosa.messenger.database.documents.NetworkFailure;
import io.zonarosa.messenger.database.model.MessageRecord;
import io.zonarosa.messenger.recipients.Recipient;

public final class RecipientDeliveryStatus {

  enum Status {
    UNKNOWN, PENDING, SENT, DELIVERED, READ, VIEWED, SKIPPED,
  }

  private final MessageRecord       messageRecord;
  private final Recipient           recipient;
  private final Status              deliveryStatus;
  private final boolean             isUnidentified;
  private final long                timestamp;
  private final NetworkFailure      networkFailure;
  private final IdentityKeyMismatch keyMismatchFailure;

  RecipientDeliveryStatus(@NonNull MessageRecord messageRecord, @NonNull Recipient recipient, @NonNull Status deliveryStatus, boolean isUnidentified, long timestamp, @Nullable NetworkFailure networkFailure, @Nullable IdentityKeyMismatch keyMismatchFailure) {
    this.messageRecord      = messageRecord;
    this.recipient          = recipient;
    this.deliveryStatus     = deliveryStatus;
    this.isUnidentified     = isUnidentified;
    this.timestamp          = timestamp;
    this.networkFailure     = networkFailure;
    this.keyMismatchFailure = keyMismatchFailure;
  }

  public @NonNull MessageRecord getMessageRecord() {
    return messageRecord;
  }

  public @NonNull Status getDeliveryStatus() {
    return deliveryStatus;
  }

  public boolean isUnidentified() {
    return isUnidentified;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public @NonNull Recipient getRecipient() {
    return recipient;
  }

  public @Nullable NetworkFailure getNetworkFailure() {
    return networkFailure;
  }

  public @Nullable IdentityKeyMismatch getKeyMismatchFailure() {
    return keyMismatchFailure;
  }
}
