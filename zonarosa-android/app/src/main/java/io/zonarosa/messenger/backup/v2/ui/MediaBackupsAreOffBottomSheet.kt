/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.backup.v2.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.os.BundleCompat
import io.zonarosa.core.ui.R
import io.zonarosa.core.ui.compose.BottomSheets
import io.zonarosa.core.ui.compose.Buttons
import io.zonarosa.core.ui.compose.DayNightPreviews
import io.zonarosa.core.ui.compose.Previews
import io.zonarosa.core.ui.compose.ZonaRosaIcons
import io.zonarosa.core.util.gibiBytes
import io.zonarosa.core.util.money.FiatMoney
import io.zonarosa.messenger.backup.v2.ui.subscription.MessageBackupsType
import io.zonarosa.messenger.billing.upgrade.UpgradeToPaidTierBottomSheet
import io.zonarosa.messenger.payments.FiatMoneyUtil
import java.math.BigDecimal
import java.util.Currency
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class MediaBackupsAreOffBottomSheet : UpgradeToPaidTierBottomSheet() {

  companion object {
    private const val ARG_ALERT = "alert"
  }

  private val backupAlert: BackupAlert by lazy(LazyThreadSafetyMode.NONE) {
    BundleCompat.getParcelable(requireArguments(), ARG_ALERT, BackupAlert::class.java)!!
  }

  @Composable
  override fun UpgradeSheetContent(
    paidBackupType: MessageBackupsType.Paid,
    freeBackupType: MessageBackupsType.Free,
    isSubscribeEnabled: Boolean,
    onSubscribeClick: () -> Unit
  ) {
    SheetContent(
      backupAlert as BackupAlert.MediaBackupsAreOff,
      paidBackupType,
      isSubscribeEnabled,
      onSubscribeClick,
      onNotNowClick = { dismissAllowingStateLoss() }
    )
  }
}

@Composable
private fun SheetContent(
  mediaBackupsAreOff: BackupAlert.MediaBackupsAreOff,
  paidBackupType: MessageBackupsType.Paid,
  isSubscribeEnabled: Boolean,
  onSubscribeClick: () -> Unit,
  onNotNowClick: () -> Unit
) {
  val resources = LocalContext.current.resources
  val pricePerMonth = remember(paidBackupType) {
    FiatMoneyUtil.format(resources, paidBackupType.pricePerMonth, FiatMoneyUtil.formatOptions().trimZerosAfterDecimal())
  }

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = dimensionResource(id = R.dimen.gutter))
  ) {
    BottomSheets.Handle()

    Spacer(modifier = Modifier.size(26.dp))

    Box {
      Image(
        imageVector = ImageVector.vectorResource(id = io.zonarosa.messenger.R.drawable.image_zonarosa_backups),
        contentDescription = null,
        modifier = Modifier
          .size(80.dp)
          .padding(2.dp)
      )
      Icon(
        imageVector = ZonaRosaIcons.ErrorCircle.imageVector,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.error,
        modifier = Modifier.align(Alignment.TopEnd)
      )
    }

    val daysUntilDeletion = remember(mediaBackupsAreOff.endOfPeriodSeconds, paidBackupType.mediaTtl) {
      ((System.currentTimeMillis().milliseconds - mediaBackupsAreOff.endOfPeriodSeconds.seconds) + paidBackupType.mediaTtl).inWholeDays.toInt()
    }

    Text(
      text = stringResource(io.zonarosa.messenger.R.string.BackupAlertBottomSheet__your_backups_subscription_expired),
      style = MaterialTheme.typography.titleLarge,
      modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)
    )

    Text(
      text = pluralStringResource(id = io.zonarosa.messenger.R.plurals.BackupAlertBottomSheet__your_backup_plan_has_expired, daysUntilDeletion, daysUntilDeletion),
      textAlign = TextAlign.Center,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(bottom = 24.dp)
    )

    Text(
      text = stringResource(id = io.zonarosa.messenger.R.string.BackupAlertBottomSheet__you_can_begin_paying_for_backups_again),
      textAlign = TextAlign.Center,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(bottom = 36.dp)
    )

    Buttons.LargeTonal(
      enabled = isSubscribeEnabled,
      onClick = onSubscribeClick,
      modifier = Modifier
        .defaultMinSize(minWidth = 220.dp)
        .padding(bottom = 16.dp)
    ) {
      Text(text = stringResource(io.zonarosa.messenger.R.string.BackupAlertBottomSheet__subscribe_for_s_month, pricePerMonth))
    }

    TextButton(
      enabled = isSubscribeEnabled,
      onClick = onNotNowClick,
      modifier = Modifier.padding(bottom = 32.dp)
    ) {
      Text(text = stringResource(id = io.zonarosa.messenger.R.string.BackupAlertBottomSheet__not_now))
    }
  }
}

@DayNightPreviews
@Composable
private fun BackupAlertSheetContentPreviewMedia() {
  Previews.BottomSheetContentPreview {
    SheetContent(
      mediaBackupsAreOff = BackupAlert.MediaBackupsAreOff(endOfPeriodSeconds = System.currentTimeMillis().milliseconds.inWholeSeconds),
      paidBackupType = MessageBackupsType.Paid(
        pricePerMonth = FiatMoney(BigDecimal.ONE, Currency.getInstance("USD")),
        mediaTtl = 30.days,
        storageAllowanceBytes = 1.gibiBytes.inWholeBytes
      ),
      isSubscribeEnabled = true,
      onSubscribeClick = {},
      onNotNowClick = {}
    )
  }
}
