//
// Copyright 2023 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.metadata.protocol;

import static io.zonarosa.libzonarosa.internal.FilterExceptions.filterExceptions;

import java.util.Optional;
import io.zonarosa.libzonarosa.internal.Native;
import io.zonarosa.libzonarosa.internal.NativeHandleGuard;
import io.zonarosa.libzonarosa.metadata.InvalidMetadataMessageException;
import io.zonarosa.libzonarosa.metadata.certificate.InvalidCertificateException;
import io.zonarosa.libzonarosa.metadata.certificate.SenderCertificate;
import io.zonarosa.libzonarosa.protocol.message.CiphertextMessage;

public class UnidentifiedSenderMessageContent extends NativeHandleGuard.SimpleOwner {
  // Must be kept in sync with sealed_sender.proto.
  public static final int CONTENT_HINT_DEFAULT = 0;
  public static final int CONTENT_HINT_RESENDABLE = 1;
  public static final int CONTENT_HINT_IMPLICIT = 2;

  @Override
  protected void release(long nativeHandle) {
    Native.UnidentifiedSenderMessageContent_Destroy(nativeHandle);
  }

  public UnidentifiedSenderMessageContent(long nativeHandle) {
    super(nativeHandle);
  }

  public UnidentifiedSenderMessageContent(byte[] serialized)
      throws InvalidMetadataMessageException, InvalidCertificateException {
    super(createNativeFrom(serialized));
  }

  private static long createNativeFrom(byte[] serialized)
      throws InvalidMetadataMessageException, InvalidCertificateException {
    try {
      return Native.UnidentifiedSenderMessageContent_Deserialize(serialized);
    } catch (Exception e) {
      throw new InvalidMetadataMessageException(e);
    }
  }

  public UnidentifiedSenderMessageContent(
      CiphertextMessage message,
      SenderCertificate senderCertificate,
      int contentHint,
      Optional<byte[]> groupId) {
    super(
        UnidentifiedSenderMessageContent.createNativeFrom(
            message, senderCertificate, contentHint, groupId));
  }

  private static long createNativeFrom(
      CiphertextMessage message,
      SenderCertificate senderCertificate,
      int contentHint,
      Optional<byte[]> groupId) {
    try (NativeHandleGuard certificateGuard = new NativeHandleGuard(senderCertificate)) {
      return filterExceptions(
          () ->
              Native.UnidentifiedSenderMessageContent_New(
                  message, certificateGuard.nativeHandle(), contentHint, groupId.orElse(null)));
    }
  }

  public int getType() {
    return filterExceptions(
        () -> guardedMapChecked(Native::UnidentifiedSenderMessageContent_GetMsgType));
  }

  public SenderCertificate getSenderCertificate() {
    return new SenderCertificate(
        filterExceptions(
            () -> guardedMapChecked(Native::UnidentifiedSenderMessageContent_GetSenderCert)));
  }

  public byte[] getContent() {
    return filterExceptions(
        () -> guardedMapChecked(Native::UnidentifiedSenderMessageContent_GetContents));
  }

  public byte[] getSerialized() {
    return filterExceptions(
        () -> guardedMapChecked(Native::UnidentifiedSenderMessageContent_GetSerialized));
  }

  public int getContentHint() {
    return filterExceptions(
        () -> guardedMapChecked(Native::UnidentifiedSenderMessageContent_GetContentHint));
  }

  public Optional<byte[]> getGroupId() {
    return Optional.ofNullable(
        filterExceptions(
            () -> guardedMapChecked(Native::UnidentifiedSenderMessageContent_GetGroupId)));
  }
}
