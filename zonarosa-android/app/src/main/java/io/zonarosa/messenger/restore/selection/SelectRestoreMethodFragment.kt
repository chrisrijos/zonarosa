/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.restore.selection

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.zonarosa.core.ui.compose.ComposeFragment
import io.zonarosa.core.ui.compose.Dialogs
import io.zonarosa.messenger.MainActivity
import io.zonarosa.messenger.R
import io.zonarosa.messenger.registration.data.QuickRegistrationRepository
import io.zonarosa.messenger.registration.ui.restore.RemoteRestoreActivity
import io.zonarosa.messenger.registration.ui.restore.RestoreMethod
import io.zonarosa.messenger.registration.ui.restore.SelectRestoreMethodScreen
import io.zonarosa.messenger.restore.RestoreViewModel
import io.zonarosa.messenger.util.navigation.safeNavigate
import io.zonarosa.service.api.provisioning.RestoreMethod as ApiRestoreMethod

/**
 * Provide options to select restore/transfer operation during quick/post registration.
 */
class SelectRestoreMethodFragment : ComposeFragment() {

  private val viewModel: RestoreViewModel by activityViewModels()

  @Composable
  override fun FragmentContent() {
    var showSkipRestoreWarning by remember { mutableStateOf(false) }

    SelectRestoreMethodScreen(
      restoreMethods = viewModel.getAvailableRestoreMethods(),
      onRestoreMethodClicked = this::startRestoreMethod,
      onSkip = { showSkipRestoreWarning = true }
    ) {
      if (viewModel.showStorageAccountRestoreProgress) {
        Dialogs.IndeterminateProgressDialog()
      } else if (showSkipRestoreWarning) {
        Dialogs.SimpleAlertDialog(
          title = stringResource(R.string.SelectRestoreMethodFragment__skip_restore_title),
          body = stringResource(R.string.SelectRestoreMethodFragment__skip_restore_warning),
          confirm = stringResource(R.string.SelectRestoreMethodFragment__skip_restore),
          dismiss = stringResource(android.R.string.cancel),
          onConfirm = {
            lifecycleScope.launch {
              viewModel.skipRestore()
              viewModel.performStorageServiceAccountRestoreIfNeeded()

              if (isActive) {
                withContext(Dispatchers.Main) {
                  startActivity(MainActivity.clearTop(requireContext()))
                  activity?.finish()
                }
              }
            }
          },
          onDismiss = { showSkipRestoreWarning = false },
          confirmColor = MaterialTheme.colorScheme.error,
          properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        )
      }
    }
  }

  private fun startRestoreMethod(method: RestoreMethod) {
    val apiRestoreMethod = when (method) {
      RestoreMethod.FROM_ZONAROSA_BACKUPS -> ApiRestoreMethod.REMOTE_BACKUP
      RestoreMethod.FROM_LOCAL_BACKUP_V1, RestoreMethod.FROM_LOCAL_BACKUP_V2 -> ApiRestoreMethod.LOCAL_BACKUP
      RestoreMethod.FROM_OLD_DEVICE -> ApiRestoreMethod.DEVICE_TRANSFER
    }

    lifecycleScope.launch {
      QuickRegistrationRepository.setRestoreMethodForOldDevice(apiRestoreMethod)
    }

    when (method) {
      RestoreMethod.FROM_ZONAROSA_BACKUPS -> {
        if (viewModel.hasRestoredBackupDataFromQr()) {
          startActivity(RemoteRestoreActivity.getIntent(requireContext()))
        } else {
          findNavController().safeNavigate(SelectRestoreMethodFragmentDirections.goToPostRestoreEnterBackupKey())
        }
      }
      RestoreMethod.FROM_OLD_DEVICE -> findNavController().safeNavigate(SelectRestoreMethodFragmentDirections.goToDeviceTransfer())
      RestoreMethod.FROM_LOCAL_BACKUP_V1 -> findNavController().safeNavigate(SelectRestoreMethodFragmentDirections.goToLocalBackupRestore())
      RestoreMethod.FROM_LOCAL_BACKUP_V2 -> error("Not currently supported")
    }
  }
}
