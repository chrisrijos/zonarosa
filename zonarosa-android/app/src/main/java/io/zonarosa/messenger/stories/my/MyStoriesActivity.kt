package io.zonarosa.messenger.stories.my

import androidx.fragment.app.Fragment
import io.zonarosa.messenger.components.FragmentWrapperActivity

class MyStoriesActivity : FragmentWrapperActivity() {
  override fun getFragment(): Fragment {
    return MyStoriesFragment()
  }
}
