package io.zonarosa.messenger.conversation

import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import io.zonarosa.core.util.dp
import io.zonarosa.messenger.R
import io.zonarosa.messenger.components.menu.ActionItem
import io.zonarosa.messenger.components.menu.ZonaRosaContextMenu
import io.zonarosa.messenger.util.DateUtils
import io.zonarosa.messenger.util.RemoteConfig
import io.zonarosa.messenger.util.toLocalDateTime
import io.zonarosa.messenger.util.toMillis
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters

class ScheduleMessageContextMenu {

  companion object {

    private val presetHours = arrayOf(8, 12, 18, 21)

    @JvmStatic
    fun show(anchor: View, container: ViewGroup, action: (Long) -> Unit): ZonaRosaContextMenu {
      val currentTime = System.currentTimeMillis()
      val scheduledTimes = getNextScheduleTimes(currentTime)
      val actionItems = scheduledTimes.map {
        if (it > 0) {
          ActionItem(getIconForTime(it), DateUtils.getScheduledMessageDateString(anchor.context, it)) {
            action(it)
          }
        } else {
          ActionItem(R.drawable.symbol_calendar_24, anchor.context.getString(R.string.ScheduledMessages_pick_time)) {
            action(it)
          }
        }
      }

      return ZonaRosaContextMenu.Builder(anchor, container)
        .offsetX(12.dp)
        .offsetY(12.dp)
        .preferredVerticalPosition(ZonaRosaContextMenu.VerticalPosition.ABOVE)
        .show(actionItems)
    }

    @DrawableRes
    private fun getIconForTime(timeMs: Long): Int {
      val dateTime = timeMs.toLocalDateTime()
      return if (dateTime.hour >= 18) {
        R.drawable.ic_nighttime_26
      } else {
        R.drawable.ic_daytime_24
      }
    }

    private fun getNextScheduleTimes(currentTimeMs: Long): List<Long> {
      var currentDateTime = currentTimeMs.toLocalDateTime()

      val timestampList = ArrayList<Long>(5)
      var presetIndex = presetHours.indexOfFirst { it > currentDateTime.hour }
      if (presetIndex == -1) {
        currentDateTime = currentDateTime.plusDays(1)
        presetIndex = 0
      }
      currentDateTime = currentDateTime.withMinute(0).withSecond(0)
      while (timestampList.size < 3) {
        currentDateTime = currentDateTime.withHour(presetHours[presetIndex])
        timestampList += currentDateTime.toMillis()
        presetIndex++
        if (presetIndex >= presetHours.size) {
          presetIndex = 0
          currentDateTime = currentDateTime.plusDays(1)
        }
      }

      if (RemoteConfig.internalUser) {
        val now = currentTimeMs.toLocalDateTime()
        if (now.dayOfWeek == DayOfWeek.FRIDAY || now.dayOfWeek == DayOfWeek.SATURDAY) {
          val nextMonday = now.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
            .withHour(8)
            .withMinute(0)
            .withSecond(0)
          timestampList += nextMonday.toMillis()
        }
      }

      timestampList += -1

      return timestampList.reversed()
    }
  }
}
