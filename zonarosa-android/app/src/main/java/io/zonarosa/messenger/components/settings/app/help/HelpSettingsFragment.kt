/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.components.settings.app.help

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import io.zonarosa.core.ui.compose.ComposeFragment
import io.zonarosa.core.ui.compose.Dividers
import io.zonarosa.core.ui.compose.Rows
import io.zonarosa.core.ui.compose.Rows.TextAndLabel
import io.zonarosa.core.ui.compose.Rows.defaultPadding
import io.zonarosa.core.ui.compose.Scaffolds
import io.zonarosa.core.ui.compose.ZonaRosaIcons
import io.zonarosa.messenger.BuildConfig
import io.zonarosa.messenger.R
import io.zonarosa.messenger.util.CommunicationActions
import io.zonarosa.messenger.util.navigation.safeNavigate

class HelpSettingsFragment : ComposeFragment() {

  @Composable
  override fun FragmentContent() {
    val navController: NavController = remember { findNavController() }

    val context = LocalContext.current

    Scaffolds.Settings(
      title = stringResource(R.string.preferences__help),
      onNavigationClick = { navController.popBackStack() },
      navigationIcon = ZonaRosaIcons.ArrowStart.imageVector,
      navigationContentDescription = stringResource(id = R.string.Material3SearchToolbar__close)
    ) { contentPadding ->
      LazyColumn(
        modifier = Modifier.padding(contentPadding)
      ) {
        item {
          Rows.LinkRow(
            text = stringResource(R.string.HelpSettingsFragment__support_center),
            icon = ImageVector.vectorResource(R.drawable.symbol_open_20),
            onClick = {
              CommunicationActions.openBrowserLink(context, getString(R.string.support_center_url))
            }
          )
        }

        item {
          Rows.TextRow(
            text = stringResource(id = R.string.HelpSettingsFragment__contact_us),
            onClick = {
              navController.safeNavigate(R.id.action_helpSettingsFragment_to_helpFragment)
            }
          )
        }

        item {
          Dividers.Default()
        }

        item {
          Rows.TextRow(
            text = stringResource(R.string.HelpSettingsFragment__version),
            label = BuildConfig.VERSION_NAME
          )
        }

        item {
          Rows.TextRow(
            text = stringResource(id = R.string.HelpSettingsFragment__debug_log),
            onClick = {
              navController.safeNavigate(R.id.action_helpSettingsFragment_to_submitDebugLogActivity)
            }
          )
        }

        item {
          Rows.TextRow(
            text = stringResource(id = R.string.HelpSettingsFragment__licenses),
            onClick = {
              navController.safeNavigate(R.id.action_helpSettingsFragment_to_licenseFragment)
            }
          )
        }

        item {
          Rows.LinkRow(
            text = stringResource(R.string.HelpSettingsFragment__terms_amp_privacy_policy),
            icon = ImageVector.vectorResource(R.drawable.symbol_open_20),
            onClick = {
              CommunicationActions.openBrowserLink(context, getString(R.string.terms_and_privacy_policy_url))
            }
          )
        }

        item {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(defaultPadding()),
            verticalAlignment = CenterVertically
          ) {
            TextAndLabel(
              label = StringBuilder().apply {
                append(getString(R.string.HelpFragment__copyright_zonarosa_messenger))
                append("\n")
                append(getString(R.string.HelpFragment__licenced_under_the_agplv3))
                append("\n")
                append(getString(R.string.HelpSettingsFragment__zonarosa_is_a_501c3))
              }.toString()
            )
          }
        }
      }
    }
  }
}
