package io.zonarosa.messenger.jobs

import io.zonarosa.core.util.isAbsent
import io.zonarosa.core.util.logging.Log
import io.zonarosa.libzonarosa.protocol.InvalidMessageException
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobmanager.Job
import io.zonarosa.messenger.jobmanager.JsonJobData
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.net.NotPushRegisteredException
import io.zonarosa.messenger.profiles.AvatarHelper
import io.zonarosa.messenger.providers.BlobProvider
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.service.api.crypto.AttachmentCipherInputStream.IntegrityCheck
import io.zonarosa.service.api.messages.ZonaRosaServiceAttachmentPointer
import io.zonarosa.service.api.messages.multidevice.DeviceContact
import io.zonarosa.service.api.messages.multidevice.DeviceContactsInputStream
import io.zonarosa.service.api.push.ZonaRosaServiceAddress
import io.zonarosa.service.api.push.exceptions.MissingConfigurationException
import io.zonarosa.service.api.util.AttachmentPointerUtil
import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * Sync contact data from primary device.
 */
class MultiDeviceContactSyncJob(parameters: Parameters, private val attachmentPointer: ByteArray) : BaseJob(parameters) {

  constructor(contactsAttachment: ZonaRosaServiceAttachmentPointer) : this(
    Parameters.Builder()
      .setQueue("MultiDeviceContactSyncJob")
      .build(),
    AttachmentPointerUtil.createAttachmentPointer(contactsAttachment).encode()
  )

  override fun serialize(): ByteArray? {
    return JsonJobData.Builder()
      .putBlobAsString(KEY_ATTACHMENT_POINTER, attachmentPointer)
      .serialize()
  }

  override fun getFactoryKey(): String {
    return KEY
  }

  override fun onRun() {
    if (!Recipient.self().isRegistered) {
      throw NotPushRegisteredException()
    }

    if (ZonaRosaStore.account.isPrimaryDevice) {
      Log.i(TAG, "Not linked device, aborting...")
      return
    }

    val contactAttachment: ZonaRosaServiceAttachmentPointer = AttachmentPointerUtil.createZonaRosaAttachmentPointer(attachmentPointer)

    try {
      val contactsFile: File = BlobProvider.getInstance().forNonAutoEncryptingSingleSessionOnDisk(context)
      AppDependencies.zonarosaServiceMessageReceiver
        .retrieveAttachment(contactAttachment, contactsFile, MAX_ATTACHMENT_SIZE, IntegrityCheck.forEncryptedDigest(contactAttachment.digest.get()))
        .use(this::processContactFile)
    } catch (e: MissingConfigurationException) {
      throw IOException(e)
    } catch (e: InvalidMessageException) {
      throw IOException(e)
    }
  }

  private fun processContactFile(inputStream: InputStream) {
    val deviceContacts = DeviceContactsInputStream(inputStream)
    val recipients = ZonaRosaDatabase.recipients

    var contact: DeviceContact? = deviceContacts.read()
    while (contact != null) {
      val recipient: Recipient? = if (contact.aci.isPresent) {
        Recipient.externalPush(ZonaRosaServiceAddress(contact.aci.get(), contact.e164.orElse(null)))
      } else {
        Recipient.external(contact.e164.get())
      }

      if (recipient == null) {
        continue
      }

      if (recipient.isSelf) {
        contact = deviceContacts.read()
        continue
      }

      if (contact.name.isPresent) {
        recipients.setSystemContactName(recipient.id, contact.name.get())
      }

      if (contact.expirationTimer.isPresent) {
        if (contact.expirationTimerVersion.isPresent && contact.expirationTimerVersion.get() > recipient.expireTimerVersion) {
          recipients.setExpireMessages(recipient.id, contact.expirationTimer.get(), contact.expirationTimerVersion.orElse(1))
        } else if (contact.expirationTimerVersion.isAbsent()) {
          // TODO [expireVersion] After unsupported builds expire, we can remove this branch
          recipients.setExpireMessagesWithoutIncrementingVersion(recipient.id, contact.expirationTimer.get())
        } else {
          Log.w(TAG, "[ContactSync] ${recipient.id} was synced with an old expiration timer. Ignoring. Recieved: ${contact.expirationTimerVersion.get()} Current: ${recipient.expireTimerVersion}")
        }
      }

      if (contact.avatar.isPresent) {
        try {
          AvatarHelper.setSyncAvatar(context, recipient.id, contact.avatar.get().inputStream)
        } catch (e: IOException) {
          Log.w(TAG, "Unable to set sync avatar for ${recipient.id}")
        }
      }

      contact = deviceContacts.read()
    }
  }

  override fun onShouldRetry(e: Exception): Boolean = false

  override fun onFailure() = Unit

  class Factory : Job.Factory<MultiDeviceContactSyncJob> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): MultiDeviceContactSyncJob {
      val data = JsonJobData.deserialize(serializedData)
      return MultiDeviceContactSyncJob(parameters, data.getStringAsBlob(KEY_ATTACHMENT_POINTER))
    }
  }

  companion object {
    const val KEY = "MultiDeviceContactSyncJob"
    const val KEY_ATTACHMENT_POINTER = "attachment_pointer"
    private const val MAX_ATTACHMENT_SIZE: Long = 100 * 1024 * 1024
    private val TAG = Log.tag(MultiDeviceContactSyncJob::class.java)
  }
}
