package io.zonarosa.messenger.payments.preferences.transfer;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import io.zonarosa.core.util.logging.Log;
import io.zonarosa.qr.QrScannerView;
import io.zonarosa.core.ui.logging.LoggingFragment;
import io.zonarosa.messenger.R;
import io.zonarosa.messenger.mediasend.camerax.CameraXModelBlocklist;
import io.zonarosa.messenger.payments.MobileCoinPublicAddress;
import io.zonarosa.core.util.concurrent.LifecycleDisposable;
import io.zonarosa.messenger.util.navigation.SafeNavigation;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;

public final class PaymentsTransferQrScanFragment extends LoggingFragment {

  private static final String TAG = Log.tag(PaymentsTransferQrScanFragment.class);

  private final LifecycleDisposable lifecycleDisposable = new LifecycleDisposable();

  private LinearLayout              overlay;
  private QrScannerView             scannerView;
  private PaymentsTransferViewModel viewModel;

  public PaymentsTransferQrScanFragment() {
    super(R.layout.payments_transfer_qr_scan_fragment);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    overlay     = view.findViewById(R.id.overlay);
    scannerView = view.findViewById(R.id.scanner);

    if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
      overlay.setOrientation(LinearLayout.HORIZONTAL);
    } else {
      overlay.setOrientation(LinearLayout.VERTICAL);
    }

    viewModel = new ViewModelProvider(Navigation.findNavController(view).getViewModelStoreOwner(R.id.payments_transfer), new PaymentsTransferViewModel.Factory()).get(PaymentsTransferViewModel.class);

    Toolbar toolbar = view.findViewById(R.id.payments_transfer_scan_qr);
    toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(v).popBackStack());

    scannerView.start(getViewLifecycleOwner(), CameraXModelBlocklist.isBlocklisted());

    lifecycleDisposable.bindTo(getViewLifecycleOwner());

    Disposable qrDisposable = scannerView
        .getQrData()
        .distinctUntilChanged()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(data -> {
          try {
            viewModel.postQrData(MobileCoinPublicAddress.fromQr(data).getPaymentAddressBase58());
            SafeNavigation.safeNavigate(Navigation.findNavController(requireView()), R.id.action_paymentsScanQr_pop);
          } catch (MobileCoinPublicAddress.AddressException e) {
            Log.e(TAG, "Not a valid address");
          }
        });

    lifecycleDisposable.add(qrDisposable);
  }

  @Override
  public void onConfigurationChanged(@NonNull Configuration newConfiguration) {
    super.onConfigurationChanged(newConfiguration);

    if (newConfiguration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
      overlay.setOrientation(LinearLayout.HORIZONTAL);
    } else {
      overlay.setOrientation(LinearLayout.VERTICAL);
    }
  }
}
