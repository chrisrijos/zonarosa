package io.zonarosa.messenger.devicetransfer.newdevice;

import android.os.Bundle;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.zonarosa.core.ui.logging.LoggingFragment;
import io.zonarosa.messenger.R;
import io.zonarosa.messenger.restore.RestoreActivity;

/**
 * Shown after the new device successfully completes receiving a backup from the old device.
 */
public final class NewDeviceTransferCompleteFragment extends LoggingFragment {
  public NewDeviceTransferCompleteFragment() {
    super(R.layout.new_device_transfer_complete_fragment);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    view.findViewById(R.id.new_device_transfer_complete_fragment_continue_registration)
        .setOnClickListener(v -> ((RestoreActivity) requireActivity()).onBackupCompletedSuccessfully());
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
      @Override
      public void handleOnBackPressed() { }
    });
  }
}
