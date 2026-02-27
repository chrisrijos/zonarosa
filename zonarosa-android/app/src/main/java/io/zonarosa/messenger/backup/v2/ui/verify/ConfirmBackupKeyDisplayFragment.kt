package io.zonarosa.messenger.backup.v2.ui.verify

import android.app.Activity.RESULT_OK
import androidx.compose.runtime.Composable
import io.zonarosa.core.ui.compose.ComposeFragment
import io.zonarosa.messenger.backup.v2.ui.subscription.MessageBackupsKeyVerifyScreen
import io.zonarosa.messenger.keyvalue.ZonaRosaStore

/**
 * Fragment to confirm the backup key just shown after users forget it.
 */
class ConfirmBackupKeyDisplayFragment : ComposeFragment() {

  @Composable
  override fun FragmentContent() {
    MessageBackupsKeyVerifyScreen(
      backupKey = ZonaRosaStore.account.accountEntropyPool.displayValue,
      onNavigationClick = {
        requireActivity().supportFragmentManager.popBackStack()
      },
      onNextClick = {
        ZonaRosaStore.backup.lastVerifyKeyTime = System.currentTimeMillis()
        ZonaRosaStore.backup.hasVerifiedBefore = true
        ZonaRosaStore.backup.hasSnoozedVerified = false
        requireActivity().setResult(RESULT_OK)
        requireActivity().finish()
      }
    )
  }
}
