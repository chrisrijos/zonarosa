package io.zonarosa.messenger.stories.viewer.page

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.fragment.app.Fragment
import io.zonarosa.messenger.R
import io.zonarosa.messenger.conversation.colors.AvatarColor

class TestFragment : Fragment(R.layout.test_fragment) {
  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    (view as AppCompatImageView).setImageDrawable(ColorDrawable(AvatarColor.random().colorInt()))
  }
}
