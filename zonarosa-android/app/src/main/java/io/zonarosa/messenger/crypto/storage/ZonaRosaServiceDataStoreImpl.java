package io.zonarosa.messenger.crypto.storage;

import android.content.Context;

import androidx.annotation.NonNull;

import io.zonarosa.messenger.keyvalue.ZonaRosaStore;
import io.zonarosa.service.api.ZonaRosaServiceDataStore;
import io.zonarosa.core.models.ServiceId;

public final class ZonaRosaServiceDataStoreImpl implements ZonaRosaServiceDataStore {

  private final Context                           context;
  private final ZonaRosaServiceAccountDataStoreImpl aciStore;
  private final ZonaRosaServiceAccountDataStoreImpl pniStore;

  public ZonaRosaServiceDataStoreImpl(@NonNull Context context,
                                    @NonNull ZonaRosaServiceAccountDataStoreImpl aciStore,
                                    @NonNull ZonaRosaServiceAccountDataStoreImpl pniStore)
  {
    this.context  = context;
    this.aciStore = aciStore;
    this.pniStore = pniStore;
  }

  @Override
  public ZonaRosaServiceAccountDataStoreImpl get(@NonNull ServiceId accountIdentifier) {
    if (accountIdentifier.equals(ZonaRosaStore.account().getAci())) {
      return aciStore;
    } else if (accountIdentifier.equals(ZonaRosaStore.account().getPni())) {
      return pniStore;
    } else {
      throw new IllegalArgumentException("No matching store found for " + accountIdentifier);
    }
  }

  @Override
  public ZonaRosaServiceAccountDataStoreImpl aci() {
    return aciStore;
  }

  @Override
  public ZonaRosaServiceAccountDataStoreImpl pni() {
    return pniStore;
  }

  @Override
  public boolean isMultiDevice() {
    return ZonaRosaStore.account().isMultiDevice();
  }
}
