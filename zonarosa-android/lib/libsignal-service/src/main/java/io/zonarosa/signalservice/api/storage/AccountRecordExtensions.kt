/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.api.storage

import okio.ByteString
import okio.ByteString.Companion.toByteString
import io.zonarosa.core.models.ServiceId
import io.zonarosa.core.util.isNotEmpty
import io.zonarosa.service.api.payments.PaymentsConstants
import io.zonarosa.service.api.push.ZonaRosaServiceAddress
import io.zonarosa.service.api.storage.IAPSubscriptionId.Companion.isNotNullOrBlank
import io.zonarosa.service.api.storage.StorageRecordProtoUtil.defaultAccountRecord
import io.zonarosa.service.internal.storage.protos.AccountRecord
import io.zonarosa.service.internal.storage.protos.Payments

fun AccountRecord.Builder.safeSetPayments(enabled: Boolean, entropy: ByteArray?): AccountRecord.Builder {
  val paymentsBuilder = Payments.Builder()
  val entropyPresent = entropy != null && entropy.size == PaymentsConstants.PAYMENTS_ENTROPY_LENGTH

  paymentsBuilder.enabled(enabled && entropyPresent)

  if (entropyPresent) {
    paymentsBuilder.entropy(entropy!!.toByteString())
  }

  this.payments = paymentsBuilder.build()

  return this
}
fun AccountRecord.Builder.safeSetSubscriber(subscriberId: ByteString, subscriberCurrencyCode: String): AccountRecord.Builder {
  if (subscriberId.isNotEmpty() && subscriberId.size == 32 && subscriberCurrencyCode.isNotBlank()) {
    this.subscriberId = subscriberId
    this.subscriberCurrencyCode = subscriberCurrencyCode
  } else {
    this.subscriberId = defaultAccountRecord.subscriberId
    this.subscriberCurrencyCode = defaultAccountRecord.subscriberCurrencyCode
  }

  return this
}

fun AccountRecord.Builder.safeSetBackupsSubscriber(subscriberId: ByteString, iapSubscriptionId: IAPSubscriptionId?): AccountRecord.Builder {
  if (subscriberId.isNotEmpty() && subscriberId.size == 32 && iapSubscriptionId.isNotNullOrBlank()) {
    this.backupSubscriberData = AccountRecord.IAPSubscriberData(
      subscriberId = subscriberId,
      purchaseToken = iapSubscriptionId.purchaseToken,
      originalTransactionId = iapSubscriptionId.originalTransactionId
    )
  } else {
    this.backupSubscriberData = defaultAccountRecord.backupSubscriberData
  }

  return this
}

fun AccountRecord.PinnedConversation.Contact.toZonaRosaServiceAddress(): ZonaRosaServiceAddress {
  val serviceId = ServiceId.parseOrNull(this.serviceId, this.serviceIdBinary)
  return ZonaRosaServiceAddress(serviceId, this.e164)
}
