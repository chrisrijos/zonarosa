/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.api.storage

import io.zonarosa.service.internal.storage.protos.AccountRecord
import io.zonarosa.service.internal.storage.protos.CallLinkRecord
import io.zonarosa.service.internal.storage.protos.ChatFolderRecord
import io.zonarosa.service.internal.storage.protos.ContactRecord
import io.zonarosa.service.internal.storage.protos.GroupV1Record
import io.zonarosa.service.internal.storage.protos.GroupV2Record
import io.zonarosa.service.internal.storage.protos.NotificationProfile
import io.zonarosa.service.internal.storage.protos.StorageRecord
import io.zonarosa.service.internal.storage.protos.StoryDistributionListRecord

fun ContactRecord.toZonaRosaContactRecord(storageId: StorageId): ZonaRosaContactRecord {
  return ZonaRosaContactRecord(storageId, this)
}

fun AccountRecord.toZonaRosaAccountRecord(storageId: StorageId): ZonaRosaAccountRecord {
  return ZonaRosaAccountRecord(storageId, this)
}

fun AccountRecord.Builder.toZonaRosaAccountRecord(storageId: StorageId): ZonaRosaAccountRecord {
  return ZonaRosaAccountRecord(storageId, this.build())
}

fun GroupV1Record.toZonaRosaGroupV1Record(storageId: StorageId): ZonaRosaGroupV1Record {
  return ZonaRosaGroupV1Record(storageId, this)
}

fun GroupV2Record.toZonaRosaGroupV2Record(storageId: StorageId): ZonaRosaGroupV2Record {
  return ZonaRosaGroupV2Record(storageId, this)
}

fun StoryDistributionListRecord.toZonaRosaStoryDistributionListRecord(storageId: StorageId): ZonaRosaStoryDistributionListRecord {
  return ZonaRosaStoryDistributionListRecord(storageId, this)
}

fun CallLinkRecord.toZonaRosaCallLinkRecord(storageId: StorageId): ZonaRosaCallLinkRecord {
  return ZonaRosaCallLinkRecord(storageId, this)
}

fun ChatFolderRecord.toZonaRosaChatFolderRecord(storageId: StorageId): ZonaRosaChatFolderRecord {
  return ZonaRosaChatFolderRecord(storageId, this)
}

fun NotificationProfile.toZonaRosaNotificationProfileRecord(storageId: StorageId): ZonaRosaNotificationProfileRecord {
  return ZonaRosaNotificationProfileRecord(storageId, this)
}

fun ZonaRosaContactRecord.toZonaRosaStorageRecord(): ZonaRosaStorageRecord {
  return ZonaRosaStorageRecord(id, StorageRecord(contact = this.proto))
}

fun ZonaRosaGroupV1Record.toZonaRosaStorageRecord(): ZonaRosaStorageRecord {
  return ZonaRosaStorageRecord(id, StorageRecord(groupV1 = this.proto))
}

fun ZonaRosaGroupV2Record.toZonaRosaStorageRecord(): ZonaRosaStorageRecord {
  return ZonaRosaStorageRecord(id, StorageRecord(groupV2 = this.proto))
}

fun ZonaRosaAccountRecord.toZonaRosaStorageRecord(): ZonaRosaStorageRecord {
  return ZonaRosaStorageRecord(id, StorageRecord(account = this.proto))
}

fun ZonaRosaStoryDistributionListRecord.toZonaRosaStorageRecord(): ZonaRosaStorageRecord {
  return ZonaRosaStorageRecord(id, StorageRecord(storyDistributionList = this.proto))
}

fun ZonaRosaCallLinkRecord.toZonaRosaStorageRecord(): ZonaRosaStorageRecord {
  return ZonaRosaStorageRecord(id, StorageRecord(callLink = this.proto))
}

fun ZonaRosaChatFolderRecord.toZonaRosaStorageRecord(): ZonaRosaStorageRecord {
  return ZonaRosaStorageRecord(id, StorageRecord(chatFolder = this.proto))
}

fun ZonaRosaNotificationProfileRecord.toZonaRosaStorageRecord(): ZonaRosaStorageRecord {
  return ZonaRosaStorageRecord(id, StorageRecord(notificationProfile = this.proto))
}
