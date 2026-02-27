/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.components.settings.app.subscription.manage

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import io.zonarosa.messenger.database.DatabaseObserver.InAppPaymentObserver
import io.zonarosa.messenger.database.InAppPaymentTable
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.database.model.databaseprotos.InAppPaymentData
import io.zonarosa.messenger.dependencies.AppDependencies

object ManageDonationsRepository {
  /**
   * Emits any time we see a successfully completed IDEAL payment that we've not notified the user about.
   */
  fun consumeSuccessfulIdealPayments(): Flow<InAppPaymentTable.InAppPayment> {
    return callbackFlow {
      val observer = InAppPaymentObserver {
        if (it.state == InAppPaymentTable.State.END &&
          it.data.error == null &&
          it.data.paymentMethodType == InAppPaymentData.PaymentMethodType.IDEAL &&
          !it.notified
        ) {
          trySendBlocking(it)

          ZonaRosaDatabase.inAppPayments.update(
            it.copy(notified = true)
          )
        }
      }

      AppDependencies.databaseObserver.registerInAppPaymentObserver(observer)
      awaitClose { AppDependencies.databaseObserver.unregisterObserver(observer) }
    }
  }
}
