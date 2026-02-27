package io.zonarosa.messenger.devicetransfer.newdevice

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import org.greenrobot.eventbus.EventBus
import io.zonarosa.core.ui.logging.LoggingFragment
import io.zonarosa.devicetransfer.TransferStatus
import io.zonarosa.messenger.R
import io.zonarosa.messenger.util.navigation.safeNavigate

/**
 * Shows instructions for new device to being transfer.
 */
class NewDeviceTransferInstructionsFragment : LoggingFragment(R.layout.new_device_transfer_instructions_fragment) {
  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    view
      .findViewById<View>(R.id.new_device_transfer_instructions_fragment_continue)
      .setOnClickListener { findNavController().safeNavigate(R.id.action_device_transfer_setup) }
  }

  override fun onResume() {
    super.onResume()
    EventBus.getDefault().removeStickyEvent(TransferStatus::class.java)
  }
}
