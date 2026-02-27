package io.zonarosa.messenger.payments.backup.phrase;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;

import io.zonarosa.core.util.concurrent.ZonaRosaExecutors;
import io.zonarosa.core.util.logging.Log;
import io.zonarosa.messenger.database.ZonaRosaDatabase;
import io.zonarosa.messenger.dependencies.AppDependencies;
import io.zonarosa.messenger.jobs.PaymentLedgerUpdateJob;
import io.zonarosa.messenger.jobs.ProfileUploadJob;
import io.zonarosa.messenger.keyvalue.PaymentsValues;
import io.zonarosa.messenger.keyvalue.ZonaRosaStore;
import io.zonarosa.core.util.Util;

import java.util.List;

class PaymentsRecoveryPhraseRepository {

  private static final String TAG = Log.tag(PaymentsRecoveryPhraseRepository.class);

  void restoreMnemonic(@NonNull List<String> words,
                       @NonNull Consumer<PaymentsValues.WalletRestoreResult> resultConsumer)
  {
    ZonaRosaExecutors.BOUNDED.execute(() -> {
      String                             mnemonic = Util.join(words, " ");
      PaymentsValues.WalletRestoreResult result   = ZonaRosaStore.payments().restoreWallet(mnemonic);

      switch (result) {
        case ENTROPY_CHANGED:
          Log.i(TAG, "restoreMnemonic: mnemonic resulted in entropy mismatch, flushing cached values");
          ZonaRosaDatabase.payments().deleteAll();
          AppDependencies.getPayments().closeWallet();
          updateProfileAndFetchLedger();
          break;
        case ENTROPY_UNCHANGED:
          Log.i(TAG, "restoreMnemonic: mnemonic resulted in entropy match, no flush needed.");
          updateProfileAndFetchLedger();
          break;
        case MNEMONIC_ERROR:
          Log.w(TAG, "restoreMnemonic: failed to restore wallet from given mnemonic.");
          break;
      }

      resultConsumer.accept(result);
    });
  }

  private void updateProfileAndFetchLedger() {
    AppDependencies.getJobManager()
                   .startChain(new ProfileUploadJob())
                   .then(PaymentLedgerUpdateJob.updateLedger())
                   .enqueue();
  }
}
