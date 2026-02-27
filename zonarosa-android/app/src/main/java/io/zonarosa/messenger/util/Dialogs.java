/**
 * Copyright (C) 2011 Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.zonarosa.messenger.util;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import io.zonarosa.core.ui.compose.ZonaRosaIcons;
import io.zonarosa.messenger.R;
import io.zonarosa.messenger.registration.ui.RegistrationActivity;

import java.util.Objects;

public class Dialogs {
  public static void showAlertDialog(Context context, String title, String message) {
    new MaterialAlertDialogBuilder(context)
        .setTitle(title)
        .setMessage(message)
        .setPositiveButton(android.R.string.ok, null)
        .show();
  }

  public static void showInfoDialog(Context context, String title, String message) {
    new MaterialAlertDialogBuilder(context)
        .setTitle(title)
        .setMessage(message)
        .setIcon(io.zonarosa.core.ui.R.drawable.symbol_info_24)
        .setPositiveButton(android.R.string.ok, null)
        .show();
  }

  public static void showUpgradeZonaRosaDialog(@NonNull Context context) {
    new MaterialAlertDialogBuilder(context)
        .setTitle(R.string.UpdateZonaRosaExpiredDialog__title)
        .setMessage(R.string.UpdateZonaRosaExpiredDialog__message)
        .setNegativeButton(R.string.UpdateZonaRosaExpiredDialog__cancel_action, null)
        .setPositiveButton(R.string.UpdateZonaRosaExpiredDialog__update_action, (d, w) -> {
          PlayStoreUtil.openPlayStoreOrOurApkDownloadPage(context);
        })
        .show();
  }

  public static void showReregisterZonaRosaDialog(@NonNull Context context) {
    new MaterialAlertDialogBuilder(context)
        .setTitle(R.string.ReregisterZonaRosaDialog__title)
        .setMessage(R.string.ReregisterZonaRosaDialog__message)
        .setNegativeButton(R.string.ReregisterZonaRosaDialog__cancel_action, null)
        .setPositiveButton(R.string.ReregisterZonaRosaDialog__reregister_action, (d, w) -> {
          context.startActivity(RegistrationActivity.newIntentForReRegistration(context));
        })
        .show();
  }
}
