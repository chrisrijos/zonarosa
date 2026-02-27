package io.zonarosa.messenger.components.settings.app.appearance

import android.app.Activity
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import io.zonarosa.core.util.AppUtil
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobs.EmojiSearchIndexDownloadJob
import io.zonarosa.messenger.keyvalue.SettingsValues.Theme
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.util.SplashScreenUtil

class AppearanceSettingsViewModel : ViewModel() {
  private val store = MutableStateFlow(getState())
  val state: StateFlow<AppearanceSettingsState> = store

  fun refreshState() {
    store.update { getState() }
  }

  fun setTheme(activity: Activity?, theme: Theme) {
    store.update { it.copy(theme = theme) }
    ZonaRosaStore.settings.theme = theme
    SplashScreenUtil.setSplashScreenThemeIfNecessary(activity, theme)
  }

  fun setLanguage(language: String) {
    store.update { it.copy(language = language) }
    ZonaRosaStore.settings.language = language
    EmojiSearchIndexDownloadJob.scheduleImmediately()
    AppUtil.restart(AppDependencies.application)
  }

  fun setMessageFontSize(size: Int) {
    store.update { it.copy(messageFontSize = size) }
    ZonaRosaStore.settings.messageFontSize = size
  }

  private fun getState(): AppearanceSettingsState {
    return AppearanceSettingsState(
      ZonaRosaStore.settings.theme,
      ZonaRosaStore.settings.messageFontSize,
      ZonaRosaStore.settings.language,
      ZonaRosaStore.settings.useCompactNavigationBar
    )
  }
}
