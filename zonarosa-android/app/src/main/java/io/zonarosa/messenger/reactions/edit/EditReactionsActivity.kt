package io.zonarosa.messenger.reactions.edit

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import io.zonarosa.messenger.PassphraseRequiredActivity
import io.zonarosa.messenger.R
import io.zonarosa.messenger.util.DynamicNoActionBarTheme
import io.zonarosa.messenger.util.DynamicTheme
import io.zonarosa.messenger.util.WindowUtil

class EditReactionsActivity : PassphraseRequiredActivity() {

  private val theme: DynamicTheme = DynamicNoActionBarTheme()

  override fun onCreate(savedInstanceState: Bundle?, ready: Boolean) {
    super.onCreate(savedInstanceState, ready)
    if (intent.extras?.getBoolean(ARG_FORCE_DARK_MODE) == true) {
      delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_YES
    }
    theme.onCreate(this)

    @Suppress("DEPRECATION")
    findViewById<View>(android.R.id.content).systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    WindowUtil.setStatusBarColor(window, ContextCompat.getColor(this, R.color.transparent))

    if (savedInstanceState == null) {
      supportFragmentManager.beginTransaction()
        .replace(android.R.id.content, EditReactionsFragment())
        .commit()
    }
  }

  override fun onResume() {
    super.onResume()
    theme.onResume(this)
  }

  companion object {
    const val ARG_FORCE_DARK_MODE = "arg_dark"
  }
}
