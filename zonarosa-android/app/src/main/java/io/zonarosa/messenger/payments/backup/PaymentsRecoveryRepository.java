package io.zonarosa.messenger.payments.backup;

import androidx.annotation.NonNull;

import io.zonarosa.messenger.keyvalue.ZonaRosaStore;
import io.zonarosa.messenger.payments.Mnemonic;

public final class PaymentsRecoveryRepository {
  public @NonNull Mnemonic getMnemonic() {
    return ZonaRosaStore.payments().getPaymentsMnemonic();
  }
}
