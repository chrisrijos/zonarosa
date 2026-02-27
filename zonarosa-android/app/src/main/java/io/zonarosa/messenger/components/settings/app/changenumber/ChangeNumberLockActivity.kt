/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.components.settings.app.changenumber

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.MainActivity
import io.zonarosa.messenger.PassphraseRequiredActivity
import io.zonarosa.messenger.R
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobs.AccountConsistencyWorkerJob
import io.zonarosa.messenger.jobs.PreKeysSyncJob
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.logsubmit.SubmitDebugLogActivity
import io.zonarosa.messenger.util.DynamicNoActionBarTheme
import io.zonarosa.messenger.util.DynamicTheme
import io.zonarosa.messenger.util.ZonaRosaE164Util

/**
 * A captive activity that can determine if an interrupted/erred change number request
 * caused a disparity between the server and our locally stored number.
 */
class ChangeNumberLockActivity : PassphraseRequiredActivity() {

  companion object {
    private val TAG: String = Log.tag(ChangeNumberLockActivity::class.java)

    @JvmStatic
    fun createIntent(context: Context): Intent {
      return Intent(context, ChangeNumberLockActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
      }
    }
  }

  private val viewModel: ChangeNumberViewModel by viewModels()
  private val dynamicTheme: DynamicTheme = DynamicNoActionBarTheme()

  override fun onCreate(savedInstanceState: Bundle?, ready: Boolean) {
    dynamicTheme.onCreate(this)

    onBackPressedDispatcher.addCallback(
      this,
      object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
          Log.d(TAG, "Back button press swallowed.")
        }
      }
    )

    setContentView(R.layout.activity_change_number_lock)

    reattemptChange()
  }

  private fun reattemptChange() {
    val metadata = ZonaRosaStore.misc.pendingChangeNumberMetadata
    if (metadata != null && metadata.newE164 != "") {
      viewModel.reattemptChangeLocalNumber(::onChangeStatusConfirmed, ::onFailedToGetChangeNumberStatus)
    } else {
      onMissingChangeNumberMetadata()
    }
  }

  override fun onResume() {
    super.onResume()
    dynamicTheme.onResume(this)
  }

  private fun onChangeStatusConfirmed() {
    ZonaRosaStore.misc.clearPendingChangeNumberMetadata()

    MaterialAlertDialogBuilder(this)
      .setTitle(R.string.ChangeNumberLockActivity__change_status_confirmed)
      .setMessage(getString(R.string.ChangeNumberLockActivity__your_number_has_been_confirmed_as_s, ZonaRosaE164Util.prettyPrint(ZonaRosaStore.account.e164!!)))
      .setPositiveButton(android.R.string.ok) { _, _ ->
        startActivity(MainActivity.clearTop(this))
        finish()
      }
      .setCancelable(false)
      .show()
  }

  private fun onFailedToGetChangeNumberStatus(error: Throwable) {
    Log.w(TAG, "Unable to determine status of change number", error)

    MaterialAlertDialogBuilder(this)
      .setTitle(R.string.ChangeNumberLockActivity__change_status_unconfirmed)
      .setMessage(getString(R.string.ChangeNumberLockActivity__we_could_not_determine_the_status_of_your_change_number_request, error.javaClass.simpleName))
      .setPositiveButton(R.string.ChangeNumberLockActivity__retry) { _, _ -> reattemptChange() }
      .setNegativeButton(R.string.ChangeNumberLockActivity__leave) { _, _ -> finish() }
      .setNeutralButton(R.string.ChangeNumberLockActivity__submit_debug_log) { _, _ ->
        startActivity(Intent(this, SubmitDebugLogActivity::class.java))
        finish()
      }
      .setCancelable(false)
      .show()
  }

  private fun onMissingChangeNumberMetadata() {
    Log.w(TAG, "Change number metadata is missing, gonna let it ride but this shouldn't happen")

    MaterialAlertDialogBuilder(this)
      .setTitle(R.string.ChangeNumberLockActivity__change_status_unconfirmed)
      .setMessage(getString(R.string.ChangeNumberLockActivity__we_could_not_determine_the_status_of_your_change_number_request, "MissingMetadata"))
      .setPositiveButton(android.R.string.ok) { _, _ ->
        ZonaRosaStore.misc.unlockChangeNumber()

        AppDependencies
          .jobManager
          .startChain(PreKeysSyncJob.create())
          .then(AccountConsistencyWorkerJob())
          .enqueue()

        startActivity(MainActivity.clearTop(this))
        finish()
      }
      .setNeutralButton(R.string.ChangeNumberLockActivity__submit_debug_log) { _, _ ->
        startActivity(Intent(this, SubmitDebugLogActivity::class.java))
        finish()
      }
      .setCancelable(false)
      .show()
  }
}
