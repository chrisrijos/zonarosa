package io.zonarosa.messenger.components

import android.os.Bundle
import androidx.fragment.app.Fragment
import io.zonarosa.messenger.PassphraseRequiredActivity
import io.zonarosa.messenger.R
import io.zonarosa.messenger.util.DynamicNoActionBarTheme
import io.zonarosa.messenger.util.DynamicTheme

/**
 * Activity that wraps a given fragment
 */
abstract class FragmentWrapperActivity : PassphraseRequiredActivity() {

  protected open val dynamicTheme: DynamicTheme = DynamicNoActionBarTheme()
  protected open val contentViewId: Int = R.layout.fragment_container

  override fun onPreCreate() {
    dynamicTheme.onCreate(this)
  }

  override fun onCreate(savedInstanceState: Bundle?, ready: Boolean) {
    setContentView(contentViewId)

    if (savedInstanceState == null) {
      supportFragmentManager.beginTransaction()
        .replace(R.id.fragment_container, getFragment())
        .commit()
    }
  }

  abstract fun getFragment(): Fragment

  override fun onResume() {
    super.onResume()
    dynamicTheme.onResume(this)
  }
}
