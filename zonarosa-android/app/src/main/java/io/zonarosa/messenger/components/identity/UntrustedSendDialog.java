package io.zonarosa.messenger.components.identity;


import android.content.Context;
import android.content.DialogInterface;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import io.zonarosa.messenger.R;
import io.zonarosa.messenger.crypto.ReentrantSessionLock;
import io.zonarosa.messenger.crypto.storage.ZonaRosaIdentityKeyStore;
import io.zonarosa.messenger.database.model.IdentityRecord;
import io.zonarosa.messenger.dependencies.AppDependencies;
import io.zonarosa.core.util.concurrent.SimpleTask;
import io.zonarosa.service.api.ZonaRosaSessionLock;

import java.util.List;

public class UntrustedSendDialog extends AlertDialog.Builder implements DialogInterface.OnClickListener {

  private final List<IdentityRecord> untrustedRecords;
  private final ResendListener       resendListener;

  public UntrustedSendDialog(@NonNull Context context,
                             @NonNull String message,
                             @NonNull List<IdentityRecord> untrustedRecords,
                             @NonNull ResendListener resendListener)
  {
    super(context);
    this.untrustedRecords = untrustedRecords;
    this.resendListener   = resendListener;

    setTitle(R.string.UntrustedSendDialog_send_message);
    setIcon(R.drawable.symbol_error_triangle_fill_24);
    setMessage(message);
    setPositiveButton(R.string.UntrustedSendDialog_send, this);
    setNegativeButton(android.R.string.cancel, null);
  }

  @Override
  public void onClick(DialogInterface dialog, int which) {
    final ZonaRosaIdentityKeyStore identityStore = AppDependencies.getProtocolStore().aci().identities();

    SimpleTask.run(() -> {
      try(ZonaRosaSessionLock.Lock unused = ReentrantSessionLock.INSTANCE.acquire()) {
        for (IdentityRecord identityRecord : untrustedRecords) {
          identityStore.setApproval(identityRecord.getRecipientId(), true);
        }
      }

      return null;
    }, unused -> resendListener.onResendMessage());
  }

  public interface ResendListener {
    public void onResendMessage();
  }
}
