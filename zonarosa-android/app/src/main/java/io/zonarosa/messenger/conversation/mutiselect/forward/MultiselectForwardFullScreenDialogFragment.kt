package io.zonarosa.messenger.conversation.mutiselect.forward

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.setFragmentResult
import io.zonarosa.messenger.R
import io.zonarosa.messenger.components.FullScreenDialogFragment
import io.zonarosa.messenger.conversation.mutiselect.forward.MultiselectForwardFragment.Companion.DIALOG_TITLE
import io.zonarosa.messenger.stories.Stories
import io.zonarosa.messenger.util.fragments.findListener

class MultiselectForwardFullScreenDialogFragment : FullScreenDialogFragment(), MultiselectForwardFragment.Callback {
  override fun getTitle(): Int = requireArguments().getInt(DIALOG_TITLE)

  override fun getDialogLayoutResource(): Int = R.layout.fragment_container

  override fun onFinishForwardAction() {
    findListener<Callback>()?.onFinishForwardAction()
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    if (savedInstanceState == null) {
      val fragment = MultiselectForwardFragment()
      fragment.arguments = requireArguments()

      childFragmentManager.beginTransaction()
        .replace(R.id.fragment_container, fragment)
        .commitAllowingStateLoss()
    }
  }

  override fun getDialogBackgroundColor(): Int {
    return ContextCompat.getColor(requireContext(), R.color.zonarosa_background_primary)
  }

  override fun getStorySendRequirements(): Stories.MediaTransform.SendRequirements? {
    return findListener<Callback>()?.getStorySendRequirements()
  }

  override fun getContainer(): ViewGroup {
    return requireView().findViewById(R.id.full_screen_dialog_content) as ViewGroup
  }

  override fun setResult(bundle: Bundle) {
    setFragmentResult(MultiselectForwardFragment.RESULT_KEY, bundle)
  }

  override fun exitFlow() {
    dismissAllowingStateLoss()
  }

  override fun onSearchInputFocused() = Unit

  interface Callback {
    fun onFinishForwardAction() = Unit
    fun getStorySendRequirements(): Stories.MediaTransform.SendRequirements? = null
  }
}
