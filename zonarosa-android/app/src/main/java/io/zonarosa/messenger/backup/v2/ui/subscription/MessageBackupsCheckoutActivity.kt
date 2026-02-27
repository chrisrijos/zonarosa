/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.backup.v2.ui.subscription

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.content.IntentCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import io.zonarosa.core.util.getParcelableExtraCompat
import io.zonarosa.donations.InAppPaymentType
import io.zonarosa.messenger.backup.v2.MessageBackupTier
import io.zonarosa.messenger.components.FragmentWrapperActivity
import io.zonarosa.messenger.components.settings.app.subscription.donate.CheckoutFlowActivity.Result
import io.zonarosa.messenger.components.settings.app.subscription.donate.InAppPaymentProcessorAction

/**
 * Self-contained activity for message backups checkout, which utilizes Google Play Billing
 * instead of the normal donations routes.
 */
class MessageBackupsCheckoutActivity : FragmentWrapperActivity() {

  companion object {
    private const val TIER = "tier"
    private const val RESULT_DATA = "result_data"

    fun createResultData(): Intent {
      val data = bundleOf(
        RESULT_DATA to Result(
          action = InAppPaymentProcessorAction.PROCESS_NEW_IN_APP_PAYMENT,
          inAppPaymentType = InAppPaymentType.RECURRING_BACKUP
        )
      )

      return Intent().putExtras(data)
    }
  }

  override fun onCreate(savedInstanceState: Bundle?, ready: Boolean) {
    super.onCreate(savedInstanceState, ready)

    enableEdgeToEdge()
  }

  override fun getFragment(): Fragment = MessageBackupsFlowFragment.create(
    IntentCompat.getSerializableExtra(intent, TIER, MessageBackupTier::class.java)
  )

  class Contract : ActivityResultContract<MessageBackupTier?, Result?>() {

    override fun createIntent(context: Context, input: MessageBackupTier?): Intent {
      return Intent(context, MessageBackupsCheckoutActivity::class.java).putExtra(TIER, input)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Result? {
      return intent?.getParcelableExtraCompat(RESULT_DATA, Result::class.java)
    }
  }
}
