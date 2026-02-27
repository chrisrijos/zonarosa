package io.zonarosa.messenger.util;


import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import io.zonarosa.core.util.logging.Log;
import io.zonarosa.messenger.attachments.Attachment;
import io.zonarosa.messenger.attachments.AttachmentId;
import io.zonarosa.messenger.attachments.DatabaseAttachment;
import io.zonarosa.messenger.database.NoSuchMessageException;
import io.zonarosa.messenger.database.ZonaRosaDatabase;
import io.zonarosa.messenger.database.model.MessageRecord;
import io.zonarosa.messenger.jobmanager.impl.NotInCallConstraint;
import io.zonarosa.messenger.jobs.MultiDeviceDeleteSyncJob;
import io.zonarosa.messenger.recipients.Recipient;

import java.util.Collections;
import java.util.Set;

public class AttachmentUtil {

  private static final String TAG = Log.tag(AttachmentUtil.class);

  @MainThread
  public static boolean isRestoreOnOpenPermitted(@NonNull Context context, @Nullable Attachment attachment) {
    if (attachment == null) {
      Log.w(TAG, "attachment was null, returning vacuous true");
      return true;
    }
    Set<String> allowedTypes = getAllowedAutoDownloadTypes(context);
    String      contentType  = attachment.contentType;

    if (MediaUtil.isImageType(contentType)) {
      return NotInCallConstraint.isNotInConnectedCall() && allowedTypes.contains(MediaUtil.getDiscreteMimeType(contentType));
    }
    return false;
  }
  
  @WorkerThread
  public static boolean isAutoDownloadPermitted(@NonNull Context context, @Nullable DatabaseAttachment attachment) {
    if (attachment == null) {
      Log.w(TAG, "attachment was null, returning vacuous true");
      return true;
    }

    if (!isFromTrustedConversation(context, attachment)) {
      Log.w(TAG, "Not allowing download due to untrusted conversation");
      return false;
    }

    Set<String> allowedTypes = getAllowedAutoDownloadTypes(context);
    String      contentType  = attachment.contentType;

    if (attachment.voiceNote ||
        (MediaUtil.isAudio(attachment) && TextUtils.isEmpty(attachment.fileName)) ||
        MediaUtil.isLongTextType(attachment.contentType) ||
        attachment.isSticker())
    {
      return true;
    } else if (attachment.videoGif) {
      boolean allowed = NotInCallConstraint.isNotInConnectedCall() && allowedTypes.contains("image");
      if (!allowed) {
        Log.w(TAG, "Not auto downloading. inCall: " + !NotInCallConstraint.isNotInConnectedCall() + " allowedType: " + allowedTypes.contains("image"));
      }
      return allowed;
    } else if (isNonDocumentType(contentType)) {
      boolean allowed = NotInCallConstraint.isNotInConnectedCall() && allowedTypes.contains(MediaUtil.getDiscreteMimeType(contentType));
      if (!allowed) {
        Log.w(TAG, "Not auto downloading. inCall: " + !NotInCallConstraint.isNotInConnectedCall() + " allowedType: " + allowedTypes.contains(MediaUtil.getDiscreteMimeType(contentType)));
      }
      return allowed;
    } else {
      boolean allowed = NotInCallConstraint.isNotInConnectedCall() && allowedTypes.contains("documents");
      if (!allowed) {
        Log.w(TAG, "Not auto downloading. inCall: " + !NotInCallConstraint.isNotInConnectedCall() + " allowedType: " + allowedTypes.contains("documents"));
      }
      return allowed;
    }
  }

  /**
   * Deletes the specified attachment. If its the only attachment for its linked message, the entire
   * message is deleted.
   *
   * @return message record of deleted message if a message is deleted
   */
  @WorkerThread
  public static @Nullable MessageRecord deleteAttachment(@NonNull DatabaseAttachment attachment) {
    AttachmentId attachmentId    = attachment.attachmentId;
    long         mmsId           = attachment.mmsId;
    int          attachmentCount = ZonaRosaDatabase.attachments()
                                                 .getAttachmentsForMessage(mmsId)
                                                 .size();

    MessageRecord deletedMessageRecord = null;
    if (attachmentCount <= 1) {
      deletedMessageRecord = ZonaRosaDatabase.messages().getMessageRecordOrNull(mmsId);
      ZonaRosaDatabase.messages().deleteMessage(mmsId);
    } else {
      ZonaRosaDatabase.attachments().deleteAttachment(attachmentId);
      MultiDeviceDeleteSyncJob.enqueueAttachmentDelete(ZonaRosaDatabase.messages().getMessageRecordOrNull(mmsId), attachment);
    }

    return deletedMessageRecord;
  }

  private static boolean isNonDocumentType(String contentType) {
    return
        MediaUtil.isImageType(contentType) ||
        MediaUtil.isVideoType(contentType) ||
        MediaUtil.isAudioType(contentType);
  }

  private static @NonNull Set<String> getAllowedAutoDownloadTypes(@NonNull Context context) {
    if      (NetworkUtil.isConnectedWifi(context))    return ZonaRosaPreferences.getWifiMediaDownloadAllowed(context);
    else if (NetworkUtil.isConnectedRoaming(context)) return ZonaRosaPreferences.getRoamingMediaDownloadAllowed(context);
    else if (NetworkUtil.isConnectedMobile(context))  return ZonaRosaPreferences.getMobileMediaDownloadAllowed(context);
    else                                              return Collections.emptySet();
  }

  @WorkerThread
  private static boolean isFromTrustedConversation(@NonNull Context context, @NonNull DatabaseAttachment attachment) {
    try {
      MessageRecord message = ZonaRosaDatabase.messages().getMessageRecord(attachment.mmsId);

      Recipient fromRecipient = message.getFromRecipient();
      Recipient toRecipient   = ZonaRosaDatabase.threads().getRecipientForThreadId(message.getThreadId());

      if (toRecipient != null && toRecipient.isGroup()) {
        return toRecipient.isProfileSharing() || isTrustedIndividual(fromRecipient, message);
      } else {
        return isTrustedIndividual(fromRecipient, message);
      }
    } catch (NoSuchMessageException e) {
      Log.w(TAG, "Message could not be found! Assuming not a trusted contact.");
      return false;
    }
  }

  private static boolean isTrustedIndividual(@NonNull Recipient recipient, @NonNull MessageRecord message) {
    return recipient.isSystemContact()  ||
           recipient.isProfileSharing() ||
           message.isOutgoing()         ||
           recipient.isSelf()           ||
           recipient.isReleaseNotes();
    }
  }
