package io.zonarosa.messenger.backup.v2.ui.verify

import android.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.zonarosa.core.ui.compose.ComposeFragment
import io.zonarosa.messenger.backup.v2.ui.subscription.MessageBackupsKeyRecordMode
import io.zonarosa.messenger.backup.v2.ui.subscription.MessageBackupsKeyRecordScreen
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.util.viewModel

/**
 * Fragment which displays the backup key to the user after users forget it.
 */
class ForgotBackupKeyFragment : ComposeFragment() {

  companion object {
    const val CLIPBOARD_TIMEOUT_SECONDS = 60
  }

  private val viewModel: ForgotBackupKeyViewModel by viewModel { ForgotBackupKeyViewModel() }

  @Composable
  override fun FragmentContent() {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    MessageBackupsKeyRecordScreen(
      backupKey = ZonaRosaStore.account.accountEntropyPool.displayValue,
      keySaveState = state.keySaveState,
      backupKeyCredentialManagerHandler = viewModel,
      mode = remember {
        MessageBackupsKeyRecordMode.Next(onNextClick = {
          requireActivity()
            .supportFragmentManager
            .beginTransaction()
            .add(R.id.content, ConfirmBackupKeyDisplayFragment())
            .addToBackStack(null)
            .commit()
        })
      }
    )
  }
}
